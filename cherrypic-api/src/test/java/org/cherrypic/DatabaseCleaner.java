package org.cherrypic;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.hibernate.Session;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class DatabaseCleaner implements InitializingBean {

    @PersistenceContext private EntityManager entityManager;

    private List<String> tableNames;

    @Override
    public void afterPropertiesSet() {
        entityManager.unwrap(Session.class).doWork(this::extractTableNames);
    }

    private void extractTableNames(Connection conn) {
        tableNames =
                entityManager.getMetamodel().getEntities().stream()
                        .map(e -> e.getName().replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase())
                        .toList();
    }

    public void execute() {
        entityManager.unwrap(Session.class).doWork(this::cleanTables);
    }

    private void cleanTables(Connection conn) throws SQLException {
        Statement statement = conn.createStatement();
        statement.executeUpdate("SET FOREIGN_KEY_CHECKS = 0");

        for (String name : tableNames) {
            statement.executeUpdate(String.format("TRUNCATE TABLE %s", name));
            if (columnExists(conn, name, name)) {
                statement.executeUpdate(String.format("ALTER TABLE %s AUTO_INCREMENT = 1", name));
            }
        }

        statement.executeUpdate("SET FOREIGN_KEY_CHECKS = 1");
    }

    private boolean columnExists(Connection conn, String tableName, String columnName)
            throws SQLException {
        try (ResultSet rs =
                conn.getMetaData()
                        .getColumns(
                                null, null, tableName.toUpperCase(), columnName.toUpperCase())) {
            return rs.next();
        }
    }
}
