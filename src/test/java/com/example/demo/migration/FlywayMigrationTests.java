package com.example.demo.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FlywayMigrationTests {

    private static final String URL_PROPERTY = "migration.test.url";
    private static final String USER_PROPERTY = "migration.test.user";
    private static final String PASSWORD_PROPERTY = "migration.test.password";

    @Test
    void shouldMigrateAnEmptyMySqlDatabaseAndRemainIdempotent() throws SQLException {
        String url = System.getProperty(URL_PROPERTY);
        assumeTrue(url != null && !url.isBlank(),
                () -> "Set -D" + URL_PROPERTY + " to run the isolated MySQL migration test");

        String username = System.getProperty(USER_PROPERTY, "root");
        String password = System.getProperty(PASSWORD_PROPERTY, "");
        Flyway flyway = Flyway.configure()
                .dataSource(url, username, password)
                .locations("classpath:db/migration")
                .cleanDisabled(true)
                .load();

        MigrateResult firstMigration = flyway.migrate();

        assertEquals(3, firstMigration.migrationsExecuted);
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            assertEquals(9, queryCount(connection, """
                    SELECT COUNT(*)
                    FROM information_schema.tables
                    WHERE table_schema = DATABASE()
                      AND table_name <> 'flyway_schema_history'
                    """));
            assertEquals(3, queryCount(connection,
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1 AND type = 'SQL'"));
            assertEquals(5, queryCount(connection, "SELECT COUNT(*) FROM sys_department"));
            assertEquals(5, queryCount(connection, "SELECT COUNT(*) FROM ticket_category"));
            assertEquals(4, queryCount(connection, "SELECT COUNT(*) FROM sys_role"));
            assertEquals(15, queryCount(connection, "SELECT COUNT(*) FROM sys_permission"));
            assertEquals(25, queryCount(connection, "SELECT COUNT(*) FROM sys_role_permission"));
            assertEquals(12, queryCount(connection, """
                    SELECT COUNT(*)
                    FROM information_schema.referential_constraints
                    WHERE constraint_schema = DATABASE()
                    """));
            assertEquals(8, queryCount(connection, """
                    SELECT COUNT(*)
                    FROM information_schema.table_constraints
                    WHERE constraint_schema = DATABASE()
                      AND constraint_type = 'CHECK'
                    """));
        }

        MigrateResult secondMigration = flyway.migrate();
        assertEquals(0, secondMigration.migrationsExecuted);
    }

    private long queryCount(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }
}
