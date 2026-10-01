INSERT INTO "users" ("id", "name", "email") VALUES
    ('user-1', 'Ada Lovelace', 'ada@example.com'),
    ('user-2', 'Alan Turing', 'alan@example.com');

INSERT INTO "orders" ("id", "user_id", "item", "amount") VALUES
    ('order-1', 'user-1', 'Mechanical Keyboard', 89.00),
    ('order-2', 'user-1', 'USB-C Dock', 45.50),
    ('order-3', 'user-2', 'Monitor Arm', 65.00);
