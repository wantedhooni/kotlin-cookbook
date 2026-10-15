# Architecture Decision Notes

## ADR-001: 외부 결제 승인과 DB transaction 분리

외부 VAN HTTP 호출을 DB transaction 내부에서 수행하면 외부 지연 동안 DB connection/lock을 점유한다. 또한 외부 승인과 DB commit은 atomic하지 않다. 따라서 `start -> VAN -> confirm`으로 transaction을 짧게 분리하고 confirm 실패 시 cancellation으로 보상한다.

## ADR-002: 결제 멱등성은 unique constraint가 최종 방어선

애플리케이션의 선조회만으로 동시 요청 race를 막을 수 없다. `payments.idempotency_key`에 DB unique constraint를 둔다. 프로덕션에서는 unique violation을 기존 결제 조회로 변환해 동일 응답을 반환한다.

## ADR-003: 정산 원장은 debit = credit invariant

정산 총액을 하나의 차변과 seller payable / fee / withholding tax 세 대변으로 분해한다. 이를 통해 금액 보존을 자동 대사할 수 있다.

## ADR-004: 지급은 원장과 별도 상태머신

은행 API는 DB와 분산 transaction을 구성하지 않는다. `READY -> PAID | PAYOUT_FAILED` 상태를 저장하고, timeout/unknown은 조회 기반 reconciliation 후 retry해야 한다.
