package com.example.trading.orderservice.adapter.`in`.web

import com.example.trading.domain.*
import com.example.trading.orderservice.application.*
import com.example.trading.orderservice.adapter.out.persistence.OrderCorrectionEntity
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.util.UUID

@RestController
@RequestMapping("/api/v1/orders")
class OrderController(private val service: OrderApplicationService) {
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    fun place(@Valid @RequestBody request: PlaceOrderRequest): OrderResponse = service.place(request.toCommand()).toResponse()
    @PostMapping("/{orderId}/cancel") fun cancel(@PathVariable orderId: UUID): OrderResponse = service.requestCancel(orderId).toResponse()
    @PostMapping("/{orderId}/corrections") @ResponseStatus(HttpStatus.ACCEPTED)
    fun correct(@PathVariable orderId: UUID, @Valid @RequestBody request: CorrectOrderRequest): CorrectionResponse = service.requestCorrection(orderId, CorrectOrderCommand(request.newPrice)).toResponse()
    @GetMapping("/{orderId}") fun get(@PathVariable orderId: UUID): OrderResponse = service.get(orderId).toResponse()
    @GetMapping("/{orderId}/corrections") fun corrections(@PathVariable orderId: UUID) = service.corrections(orderId).map { it.toResponse() }
}

data class PlaceOrderRequest(@field:NotBlank val accountId:String, @field:Pattern(regexp="\\d{6}") val symbol:String,
    val side:Side, val orderType:OrderType, @field:Min(1) val quantity:Long, val price:BigDecimal?) {
    fun toCommand()=PlaceOrderCommand(accountId,symbol,side,orderType,quantity,price)
}
data class CorrectOrderRequest(@field:DecimalMin(value="0.0", inclusive=false) val newPrice:BigDecimal)
data class OrderResponse(val orderId:UUID,val accountId:String,val symbol:String,val side:Side,val orderType:OrderType,val quantity:Long,val price:BigDecimal?,val filledQuantity:Long,val remainingQuantity:Long,val status:OrderStatus)
data class CorrectionResponse(val correctionId:UUID,val orderId:UUID,val oldPrice:BigDecimal,val newPrice:BigDecimal,val status:String)
private fun Order.toResponse()=OrderResponse(id,accountId,symbol,side,orderType,quantity,price,filledQuantity,remainingQuantity,status)
private fun OrderCorrectionEntity.toResponse()=CorrectionResponse(correctionId,orderId,oldPrice,newPrice,status.name)
