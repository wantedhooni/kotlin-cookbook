package com.example.trading.mockkrx.adapter.`in`.kafka

import com.example.trading.event.*
import com.example.trading.mockkrx.domain.MockMatchingEngine
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@Component
class MockKrxListener(private val mapper:ObjectMapper,private val kafka:KafkaTemplate<String,String>,private val engine:MockMatchingEngine) {
    data class Pending(var order:KrxOrderRequest,var remaining:Long,var nextFillAt:Instant)
    private val received=ConcurrentHashMap.newKeySet<UUID>()
    private val pending=ConcurrentHashMap<UUID,Pending>()
    private val earlyCancels=ConcurrentHashMap.newKeySet<UUID>()
    private val earlyCorrections=ConcurrentHashMap<UUID,KrxCorrectionRequest>()

    @KafkaListener(topics=[Topics.KRX_ORDER_REQUEST],groupId="mock-krx-orders") @Synchronized
    fun order(payload:String) {
        var request=mapper.readValue(payload,KrxOrderRequest::class.java)
        if(!received.add(request.orderId)) return
        if(earlyCancels.remove(request.orderId)){ send(Topics.ORDER_CANCELED,request.orderId.toString(),OrderCanceledEvent(UUID.randomUUID(),request.orderId,request.quantity,Instant.now())); return }
        earlyCorrections.remove(request.orderId)?.let { correction ->
            val old=requireNotNull(request.price)
            request=request.copy(price=correction.newPrice)
            send(Topics.ORDER_CORRECTED,request.orderId.toString(),OrderCorrectedEvent(UUID.randomUUID(),correction.correctionId,request.orderId,old,correction.newPrice,request.quantity,Instant.now()))
        }
        val first=engine.firstFillQuantity(request.quantity); publishExecution(request,first)
        val remain=request.quantity-first
        if(remain>0) pending[request.orderId]=Pending(request,remain,Instant.now().plusSeconds(2))
    }

    @KafkaListener(topics=[Topics.KRX_CORRECTION_REQUEST],groupId="mock-krx-correction") @Synchronized
    fun correction(payload:String) {
        val request=mapper.readValue(payload,KrxCorrectionRequest::class.java)
        val p=pending[request.orderId]
        if(p!=null){
            val old=requireNotNull(p.order.price)
            p.order=p.order.copy(price=request.newPrice)
            p.nextFillAt=Instant.now().plusSeconds(2)
            send(Topics.ORDER_CORRECTED,request.orderId.toString(),OrderCorrectedEvent(UUID.randomUUID(),request.correctionId,request.orderId,old,request.newPrice,p.remaining,Instant.now()))
        } else if(!received.contains(request.orderId)) {
            earlyCorrections[request.orderId]=request
        } else {
            send(Topics.ORDER_CORRECTION_REJECTED,request.orderId.toString(),OrderCorrectionRejectedEvent(UUID.randomUUID(),request.correctionId,request.orderId,"order is already fully filled",Instant.now()))
        }
    }

    @KafkaListener(topics=[Topics.KRX_CANCEL_REQUEST],groupId="mock-krx-cancel") @Synchronized
    fun cancel(payload:String){
        val request=mapper.readValue(payload,KrxCancelRequest::class.java); val removed=pending.remove(request.orderId)
        if(removed!=null) send(Topics.ORDER_CANCELED,request.orderId.toString(),OrderCanceledEvent(UUID.randomUUID(),request.orderId,removed.remaining,Instant.now()))
        else if(!received.contains(request.orderId)) earlyCancels.add(request.orderId)
        else send(Topics.ORDER_CANCEL_REJECTED,request.orderId.toString(),OrderCancelRejectedEvent(UUID.randomUUID(),request.orderId,"order is already fully filled",Instant.now()))
    }

    @Scheduled(fixedDelay=250) @Synchronized
    fun completePendingOrders(){ val now=Instant.now(); pending.entries.filter{!it.value.nextFillAt.isAfter(now)}.forEach{(id,p)->if(pending.remove(id,p))publishExecution(p.order,p.remaining)} }
    private fun publishExecution(order:KrxOrderRequest,quantity:Long){ send(Topics.KRX_EXECUTION,order.orderId.toString(),KrxExecutionEvent(UUID.randomUUID(),"KRX-${UUID.randomUUID()}",order.orderId,order.accountId,order.symbol,order.side,quantity,engine.executionPrice(order),Instant.now())) }
    private fun send(topic:String,key:String,value:Any){ kafka.send(topic,key,mapper.writeValueAsString(value)).get(5,TimeUnit.SECONDS) }
}
