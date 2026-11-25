package com.stackforge.kidtrack.platform.notifications.infrastructure.persistence.schema;

import java.sql.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Databases created by earlier versions may store the notification type in a MySQL {@code ENUM} column, which
 * rejects the new types and which Hibernate's schema update does not widen. This turns it into a text column.
 * It does nothing on other databases or once the column is already text.
 */
@Component
@Order(0)
public class NotificationTypeColumnUpgrade implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationTypeColumnUpgrade.class);

    private final JdbcTemplate jdbcTemplate;

    public NotificationTypeColumnUpgrade(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        ConnectionCallback<String> productName = (Connection connection) ->
                connection.getMetaData().getDatabaseProductName();
        if (!"MySQL".equalsIgnoreCase(jdbcTemplate.execute(productName))) {
            return;
        }
        var types = jdbcTemplate.queryForList("select data_type from information_schema.columns "
                + "where table_schema = database() and table_name = 'notifications' and column_name = 'type'",
                String.class);
        if (types.stream().anyMatch("enum"::equalsIgnoreCase)) {
            jdbcTemplate.execute("alter table notifications modify column type varchar(20) not null");
            LOGGER.info("The notification type column was upgraded to varchar.");
        }
    }
}
