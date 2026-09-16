-- pg_bulk_demo_data.sql
-- PostgreSQL 대용량 PG 정산 테스트 데이터
--
-- 기본 생성량: 1,000,000 PG 결제
--
-- 데이터 분포
--   - 정상 승인: 대부분
--   - 부분취소 1회: seq % 20 = 0
--   - 부분취소 2회: seq % 50 = 0
--   - 전체취소: seq % 100 = 0
--   - 주문 없음: seq % 1000 = 0
--   - 주문금액/PG 승인금액 불일치: seq % 500 = 0 (주문 없음 제외)
--
-- pg_settlement은 비워둡니다.
-- Spring Batch ItemReader -> Processor -> Writer 결과 확인용입니다.
--
-- 주의:
-- clean_existing = true이면 기존 테스트/정산 데이터가 모두 삭제됩니다.

CREATE TEMP TABLE demo_config (
    row_count      BIGINT  NOT NULL,
    clean_existing BOOLEAN NOT NULL
);

-- 생성 건수 변경은 아래 row_count만 수정하면 됩니다.
-- 100만 건  : 1000000
-- 500만 건  : 5000000
-- 1000만 건 : 10000000
INSERT INTO demo_config(row_count, clean_existing)
VALUES (1000000, true);


-- ============================================================
-- 0. 기존 데이터 초기화
-- ============================================================

DO $$
BEGIN
    IF (SELECT clean_existing FROM demo_config LIMIT 1) THEN
        TRUNCATE TABLE
            pg_settlement,
            pg_transaction,
            pg_payment,
            orders
        RESTART IDENTITY CASCADE;
END IF;
END
$$;


-- 대량 적재 속도 개선용.
-- 현재 트랜잭션에만 적용됩니다.
BEGIN;

SET LOCAL synchronous_commit = off;


-- ============================================================
-- 1. orders
-- ============================================================
--
-- ORDER-0000000001 ~ ORDER-0001000000
--
-- 금액:
--   10,000 ~ 200,000 범위
-- ============================================================

INSERT INTO orders (
    order_no,
    order_amount
)
SELECT
    'ORDER-' || LPAD(g::TEXT, 10, '0')                         AS order_no,
    (10000 + ((g - 1) % 191) * 1000)::NUMERIC(19, 2)         AS order_amount
FROM generate_series(
    1,
    (SELECT row_count FROM demo_config)
    ) AS g
ON CONFLICT (order_no) DO NOTHING;


-- ============================================================
-- 2. pg_payment
-- ============================================================
--
-- 테스트 케이스:
--
-- 1) seq % 1000 = 0
--      orders에 없는 ORDER-MISSING-* 생성
--      -> ORDER_NOT_FOUND 테스트
--
-- 2) seq % 500 = 0 && seq % 1000 <> 0
--      주문금액보다 PG 승인금액을 1,000원 크게 생성
--      -> PAYMENT_AMOUNT_MISMATCH 테스트
--
-- 3) seq % 100 = 0
--      전체취소
--
-- 4) seq % 50 = 0
--      부분취소 2회, 총 40%
--
-- 5) seq % 20 = 0
--      부분취소 1회, 총 30%
--
-- 6) 나머지
--      정상 승인
-- ============================================================

WITH source AS (
    SELECT
        g,
        (10000 + ((g - 1) % 191) * 1000)::NUMERIC(19, 2) AS order_amount
FROM generate_series(
    1,
    (SELECT row_count FROM demo_config)
    ) AS g
    ),
    payment_source AS (
SELECT
    g,

    CASE
    WHEN g % 1000 = 0
    THEN 'ORDER-MISSING-' || LPAD(g::TEXT, 10, '0')
    ELSE 'ORDER-' || LPAD(g::TEXT, 10, '0')
    END AS order_no,

    CASE
    WHEN g % 500 = 0
    AND g % 1000 <> 0
    THEN order_amount + 1000
    ELSE order_amount
    END::NUMERIC(19, 2) AS approved_amount

FROM source
    )
INSERT INTO pg_payment (
    pg_tid,
    order_no,
    approved_amount,
    canceled_amount,
    status
)
SELECT
    'PG-TID-' || LPAD(g::TEXT, 10, '0') AS pg_tid,

    order_no,

    approved_amount,

    CASE
        WHEN g % 100 = 0
            THEN approved_amount

        WHEN g % 50 = 0
            THEN ROUND(approved_amount * 0.40, 2)

        WHEN g % 20 = 0
            THEN ROUND(approved_amount * 0.30, 2)

        ELSE 0
        END::NUMERIC(19, 2) AS canceled_amount,

    CASE
        WHEN g % 100 = 0
            THEN 'CANCELLED'

        WHEN g % 50 = 0
        OR g % 20 = 0
            THEN 'PARTIALLY_CANCELLED'

        ELSE 'PAID'
END AS status

FROM payment_source
ON CONFLICT (pg_tid) DO NOTHING;


-- ============================================================
-- 3. 승인 transaction
-- ============================================================
--
-- 모든 PG 결제에 승인 이벤트 1건
-- 수수료: 승인금액의 3%
-- 승인 수수료이므로 fee_amount는 양수
-- 정산일: 2026-09-12
-- ============================================================

INSERT INTO pg_transaction (
    pg_event_id,
    pg_payment_id,
    transaction_type,
    amount,
    fee_amount,
    occurred_at,
    settlement_date
)
SELECT
    'EVENT-APPROVAL-' || p.id                         AS pg_event_id,
    p.id                                              AS pg_payment_id,
    'APPROVAL'                                        AS transaction_type,
    p.approved_amount                                 AS amount,
    ROUND(p.approved_amount * 0.03, 2)               AS fee_amount,
    TIMESTAMP '2026-09-12 00:00:00'
        + ((p.id % 86400) * INTERVAL '1 second')      AS occurred_at,
    DATE '2026-09-12'                                 AS settlement_date
FROM pg_payment p
WHERE p.pg_tid LIKE 'PG-TID-%'
ON CONFLICT (pg_event_id) DO NOTHING;


-- ============================================================
-- 4. 부분취소 1회
-- ============================================================
--
-- 대상:
--   seq % 20 = 0
--   단, seq % 50 != 0
--   단, seq % 100 != 0
--
-- 취소금액: 승인금액의 30%
-- 수수료 반환: 취소금액의 3% -> 음수
-- 정산일: 2026-09-13
-- ============================================================

INSERT INTO pg_transaction (
    pg_event_id,
    pg_payment_id,
    transaction_type,
    amount,
    fee_amount,
    occurred_at,
    settlement_date
)
SELECT
    'EVENT-PARTIAL-CANCEL-1-' || p.id,
    p.id,
    'PARTIAL_CANCEL',

    ROUND(
            p.approved_amount * 0.30,
            2
    )::NUMERIC(19, 2),

    -ROUND(
            p.approved_amount * 0.30 * 0.03,
            2
     )::NUMERIC(19, 2),

    TIMESTAMP '2026-09-13 00:00:00'
        + ((p.id % 86400) * INTERVAL '1 second'),

    DATE '2026-09-13'

FROM pg_payment p
WHERE p.id % 20 = 0
  AND p.id % 50 <> 0
  AND p.id % 100 <> 0
  AND p.pg_tid LIKE 'PG-TID-%'
ON CONFLICT (pg_event_id) DO NOTHING;


-- ============================================================
-- 5. 부분취소 2회 - 첫 번째 취소
-- ============================================================
--
-- 대상:
--   seq % 50 = 0
--   단, seq % 100 != 0
--
-- 첫 번째 부분취소: 승인금액의 25%
-- 정산일: 2026-09-13
-- ============================================================

INSERT INTO pg_transaction (
    pg_event_id,
    pg_payment_id,
    transaction_type,
    amount,
    fee_amount,
    occurred_at,
    settlement_date
)
SELECT
    'EVENT-PARTIAL-CANCEL-1-' || p.id,
    p.id,
    'PARTIAL_CANCEL',

    ROUND(
            p.approved_amount * 0.25,
            2
    )::NUMERIC(19, 2),

    -ROUND(
            p.approved_amount * 0.25 * 0.03,
            2
     )::NUMERIC(19, 2),

    TIMESTAMP '2026-09-13 00:00:00'
        + ((p.id % 86400) * INTERVAL '1 second'),

    DATE '2026-09-13'

FROM pg_payment p
WHERE p.id % 50 = 0
  AND p.id % 100 <> 0
  AND p.pg_tid LIKE 'PG-TID-%'
ON CONFLICT (pg_event_id) DO NOTHING;


-- ============================================================
-- 6. 부분취소 2회 - 두 번째 취소
-- ============================================================
--
-- 두 번째 부분취소: 승인금액의 15%
-- 누적 취소: 25% + 15% = 40%
-- 정산일: 2026-09-14
-- ============================================================

INSERT INTO pg_transaction (
    pg_event_id,
    pg_payment_id,
    transaction_type,
    amount,
    fee_amount,
    occurred_at,
    settlement_date
)
SELECT
    'EVENT-PARTIAL-CANCEL-2-' || p.id,
    p.id,
    'PARTIAL_CANCEL',

    ROUND(
            p.approved_amount * 0.15,
            2
    )::NUMERIC(19, 2),

    -ROUND(
            p.approved_amount * 0.15 * 0.03,
            2
     )::NUMERIC(19, 2),

    TIMESTAMP '2026-09-14 00:00:00'
        + ((p.id % 86400) * INTERVAL '1 second'),

    DATE '2026-09-14'

FROM pg_payment p
WHERE p.id % 50 = 0
  AND p.id % 100 <> 0
  AND p.pg_tid LIKE 'PG-TID-%'
ON CONFLICT (pg_event_id) DO NOTHING;


-- ============================================================
-- 7. 전체취소
-- ============================================================
--
-- 대상:
--   seq % 100 = 0
--
-- 취소금액: 승인금액 전체
-- 승인 시 부과된 3% 수수료 전액 반환
-- 정산일: 2026-09-13
-- ============================================================

INSERT INTO pg_transaction (
    pg_event_id,
    pg_payment_id,
    transaction_type,
    amount,
    fee_amount,
    occurred_at,
    settlement_date
)
SELECT
    'EVENT-CANCEL-' || p.id,
    p.id,
    'CANCEL',

    p.approved_amount,

    -ROUND(
            p.approved_amount * 0.03,
            2
     )::NUMERIC(19, 2),

    TIMESTAMP '2026-09-13 12:00:00'
        + ((p.id % 43200) * INTERVAL '1 second'),

    DATE '2026-09-13'

FROM pg_payment p
WHERE p.id % 100 = 0
  AND p.pg_tid LIKE 'PG-TID-%'
ON CONFLICT (pg_event_id) DO NOTHING;


COMMIT;


-- ============================================================
-- 8. 통계 갱신
-- ============================================================

ANALYZE orders;
ANALYZE pg_payment;
ANALYZE pg_transaction;


-- ============================================================
-- 9. 생성 결과 확인
-- ============================================================

SELECT 'orders' AS table_name, COUNT(*) AS row_count
FROM orders

UNION ALL

SELECT 'pg_payment', COUNT(*)
FROM pg_payment

UNION ALL

SELECT 'pg_transaction', COUNT(*)
FROM pg_transaction

UNION ALL

SELECT 'pg_settlement', COUNT(*)
FROM pg_settlement

ORDER BY table_name;


-- ============================================================
-- 10. PG 상태별 분포
-- ============================================================

SELECT
    status,
    COUNT(*) AS count
FROM pg_payment
GROUP BY status
ORDER BY status;


-- ============================================================
-- 11. Transaction 타입별 분포
-- ============================================================

SELECT
    transaction_type,
    settlement_date,
    COUNT(*) AS count,
    SUM(amount) AS amount,
    SUM(fee_amount) AS fee_amount
FROM pg_transaction
GROUP BY
    transaction_type,
    settlement_date
ORDER BY
    settlement_date,
    transaction_type;


-- ============================================================
-- 12. 주문이 없는 PG 데이터
-- ============================================================

SELECT COUNT(*) AS order_not_found_count
FROM pg_payment p
         LEFT JOIN orders o
                   ON o.order_no = p.order_no
WHERE o.id IS NULL;


-- ============================================================
-- 13. 주문금액과 승인금액 불일치
-- ============================================================

SELECT COUNT(*) AS amount_mismatch_count
FROM pg_payment p
         JOIN orders o
              ON o.order_no = p.order_no
WHERE o.order_amount <> p.approved_amount;


-- ============================================================
-- 14. 취소 누적값 검증
-- ============================================================

SELECT COUNT(*) AS invalid_cancel_amount_count
FROM (
         SELECT
             p.id,
             p.pg_tid,
             p.canceled_amount,
             COALESCE(
                     SUM(
                             CASE
                                 WHEN t.transaction_type IN (
                                                             'PARTIAL_CANCEL',
                                                             'CANCEL'
                                     )
                                     THEN t.amount
                                 ELSE 0
                                 END
                     ),
                     0
             ) AS transaction_cancel_amount
         FROM pg_payment p
                  LEFT JOIN pg_transaction t
                            ON t.pg_payment_id = p.id
         GROUP BY
             p.id,
             p.pg_tid,
             p.canceled_amount
     ) x
WHERE x.canceled_amount <> x.transaction_cancel_amount;


-- ============================================================
-- 15. Spring Batch 정산 대상 예시
-- ============================================================
--
-- settlement_date별 Reader 테스트에 사용할 수 있습니다.
--
-- 2026-09-12 : 승인
-- 2026-09-13 : 부분취소/전체취소
-- 2026-09-14 : 두 번째 부분취소
-- ============================================================

SELECT
    settlement_date,
    COUNT(*) AS transaction_count
FROM pg_transaction
GROUP BY settlement_date
ORDER BY settlement_date;
