CREATE TABLE orders (
    id            UUID           NOT NULL,
    customer_name VARCHAR(255)   NOT NULL,
    status        VARCHAR(32)    NOT NULL,
    created_at    TIMESTAMP      NOT NULL,
    updated_at    TIMESTAMP      NOT NULL,
    version       BIGINT         NOT NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id)
);
CREATE INDEX idx_orders_status ON orders (status);

CREATE TABLE order_items (
    id            UUID           NOT NULL,
    order_id      UUID           NOT NULL,
    product_id    UUID           NOT NULL,
    product_name  VARCHAR(255)  NOT NULL,
    unit_price    NUMERIC(19,4)  NOT NULL,
    quantity      INT            NOT NULL,
    CONSTRAINT pk_order_items PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT ck_order_items_qty CHECK (quantity > 0)
);
