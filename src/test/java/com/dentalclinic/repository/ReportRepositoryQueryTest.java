package com.dentalclinic.repository;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportRepositoryQueryTest {

    @Test
    void revenueQueryFiltersByVisitCreationTimestampAndAllowsEmptyResult() {
        QueryCapture capture = new QueryCapture();
        ReportRepository repository = new ReportRepository(capture::connection);
        OffsetDateTime from = OffsetDateTime.parse("2026-08-10T00:00:00+07:00");
        OffsetDateTime to = OffsetDateTime.parse("2026-08-11T00:00:00+07:00");

        assertTrue(repository.findRevenueRows(from, to).isEmpty());

        assertTrue(capture.sql.get().contains("JOIN visits v ON v.id = d.visit_id"));
        assertTrue(capture.sql.get().contains("WHERE v.created_at >= ? AND v.created_at < ?"));
        assertEquals(from, capture.parameters.get(1));
        assertEquals(to, capture.parameters.get(2));
    }

    @Test
    void medicalBookQueryFiltersByVisitCreationTimestampAndAllowsEmptyResult() {
        QueryCapture capture = new QueryCapture();
        ReportRepository repository = new ReportRepository(capture::connection);
        OffsetDateTime from = OffsetDateTime.parse("2026-08-10T00:00:00+07:00");
        OffsetDateTime to = OffsetDateTime.parse("2026-08-13T00:00:00+07:00");

        assertTrue(repository.findMedicalBookRows(from, to).isEmpty());

        assertTrue(capture.sql.get().contains("FROM visits v"));
        assertTrue(capture.sql.get().contains("WHERE v.created_at >= ? AND v.created_at < ?"));
        assertEquals(from, capture.parameters.get(1));
        assertEquals(to, capture.parameters.get(2));
    }

    private static final class QueryCapture {
        private final AtomicReference<String> sql = new AtomicReference<>();
        private final Map<Integer, Object> parameters = new HashMap<>();

        private Connection connection() {
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                    (proxy, method, arguments) -> {
                        if (method.getName().equals("prepareStatement")) {
                            sql.set((String) arguments[0]);
                            return statement();
                        }
                        return defaultValue(method.getReturnType());
                    });
        }

        private PreparedStatement statement() {
            return (PreparedStatement) Proxy.newProxyInstance(
                    PreparedStatement.class.getClassLoader(), new Class<?>[]{PreparedStatement.class},
                    (proxy, method, arguments) -> {
                        if (method.getName().equals("setObject")) {
                            parameters.put((Integer) arguments[0], arguments[1]);
                            return null;
                        }
                        if (method.getName().equals("executeQuery")) {
                            return emptyResults();
                        }
                        return defaultValue(method.getReturnType());
                    });
        }

        private static ResultSet emptyResults() {
            return (ResultSet) Proxy.newProxyInstance(
                    ResultSet.class.getClassLoader(), new Class<?>[]{ResultSet.class},
                    (proxy, method, arguments) -> method.getName().equals("next")
                            ? false : defaultValue(method.getReturnType()));
        }

        private static Object defaultValue(Class<?> type) {
            if (!type.isPrimitive()) {
                return null;
            }
            if (type == boolean.class) {
                return false;
            }
            if (type == char.class) {
                return '\0';
            }
            return 0;
        }
    }
}
