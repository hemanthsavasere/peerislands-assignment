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
    void productsTableIsCreatedViaFlyway() throws Exception {
        try (Connection c = dataSource.getConnection()) {
            ResultSet rs = c.getMetaData().getTables(null, null, "PRODUCTS", null);
            assertThat(rs.next()).isTrue();
        }
    }
}
