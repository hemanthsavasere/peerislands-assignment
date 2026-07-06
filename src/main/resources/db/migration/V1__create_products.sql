CREATE TABLE products (
    id              UUID            NOT NULL,
    name            VARCHAR(255)    NOT NULL,
    sku             VARCHAR(100)    NOT NULL,
    unit_price      NUMERIC(19,4)   NOT NULL,
    available_stock INT             NOT NULL,
    reserved_stock  INT             NOT NULL DEFAULT 0,
    version         BIGINT          NOT NULL,
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uq_products_sku UNIQUE (sku),
    CONSTRAINT ck_products_available CHECK (available_stock >= 0),
    CONSTRAINT ck_products_reserved  CHECK (reserved_stock >= 0)
);
