DROP TABLE IF EXISTS "orders";
DROP TABLE IF EXISTS "users";

CREATE TABLE "users" (
    "id"    VARCHAR(36) PRIMARY KEY,
    "name"  VARCHAR(255) NOT NULL,
    "email" VARCHAR(255) NOT NULL
);

CREATE TABLE "orders" (
    "id"      VARCHAR(36) PRIMARY KEY,
    "user_id" VARCHAR(36) NOT NULL,
    "item"    VARCHAR(255) NOT NULL,
    "amount"  DECIMAL(10, 2) NOT NULL
);
