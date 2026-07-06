package com.peerislands.orders;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MigrationsTest {
    @Autowired
    DataSource dataSource;

    @Test
    void allTablesAreCreatedViaFlyway() throws Exception {
        try (Connection c = dataSource.getConnection()) {
            assertTableExists(c, "products");
            assertTableExists(c, "orders");
            assertTableExists(c, "order_items");
            ResultSet idx = c.getMetaData().getIndexInfo(null, null, "ORDERS", false, false);
            boolean statusIndex = false;
            while (idx.next()) {
                if ("idx_orders_status".equalsIgnoreCase(idx.getString("INDEX_NAME"))) {
                    statusIndex = true;
                }
            }
            assertThat(statusIndex).as("idx_orders_status index present").isTrue();
        }
    }

    private void assertTableExists(Connection c, String name) throws Exception {
        ResultSet rs = c.getMetaData().getTables(null, null, name.toUpperCase(), null);
        assertThat(rs.next()).as("table %s exists", name).isTrue();
    }
}
