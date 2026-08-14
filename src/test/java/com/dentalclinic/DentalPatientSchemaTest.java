package com.dentalclinic;

import com.dentalclinic.config.DatabaseConfig;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DentalPatientSchemaTest {

    private static final Set<String> EXPECTED_TABLES = Set.of("patients", "visits", "doanh_thu");

    @Test
    void dentalPatientSchemaMatchesPhaseOneRequirements() throws SQLException {
        assertTrue(
                DatabaseConfig.findMissingEnvironmentVariables().isEmpty(),
                "Supabase environment variables are required for the schema integration test."
        );

        try (Connection connection = new DatabaseConfig().getConnection()) {
            assertEquals(EXPECTED_TABLES, querySet(connection, """
                    SELECT table_name
                    FROM information_schema.tables
                    WHERE table_schema = current_schema()
                      AND table_name IN ('patients', 'visits', 'doanh_thu')
                    """));

            assertEquals(EXPECTED_TABLES, querySet(connection, """
                    SELECT tc.table_name
                    FROM information_schema.table_constraints tc
                    JOIN information_schema.key_column_usage kcu
                      ON kcu.constraint_catalog = tc.constraint_catalog
                     AND kcu.constraint_schema = tc.constraint_schema
                     AND kcu.constraint_name = tc.constraint_name
                    WHERE tc.constraint_schema = current_schema()
                      AND tc.constraint_type = 'PRIMARY KEY'
                      AND kcu.column_name = 'id'
                      AND tc.table_name IN ('patients', 'visits', 'doanh_thu')
                    """));

            assertEquals(
                    Set.of("visits.patient_id->patients.id:CASCADE", "doanh_thu.visit_id->visits.id:CASCADE"),
                    querySet(connection, """
                            SELECT tc.table_name || '.' || kcu.column_name || '->'
                                   || ccu.table_name || '.' || ccu.column_name || ':' || rc.delete_rule
                            FROM information_schema.table_constraints tc
                            JOIN information_schema.key_column_usage kcu
                              ON kcu.constraint_catalog = tc.constraint_catalog
                             AND kcu.constraint_schema = tc.constraint_schema
                             AND kcu.constraint_name = tc.constraint_name
                            JOIN information_schema.constraint_column_usage ccu
                              ON ccu.constraint_catalog = tc.constraint_catalog
                             AND ccu.constraint_schema = tc.constraint_schema
                             AND ccu.constraint_name = tc.constraint_name
                            JOIN information_schema.referential_constraints rc
                              ON rc.constraint_catalog = tc.constraint_catalog
                             AND rc.constraint_schema = tc.constraint_schema
                             AND rc.constraint_name = tc.constraint_name
                            WHERE tc.constraint_schema = current_schema()
                              AND tc.constraint_type = 'FOREIGN KEY'
                              AND tc.table_name IN ('visits', 'doanh_thu')
                            """)
            );

            assertColumnNullability(connection);

            assertTrue(queryBoolean(connection, """
                    SELECT EXISTS (
                        SELECT 1
                        FROM pg_constraint constraint_definition
                        JOIN pg_class table_definition ON table_definition.oid = constraint_definition.conrelid
                        JOIN pg_namespace schema_definition ON schema_definition.oid = table_definition.relnamespace
                        WHERE schema_definition.nspname = current_schema()
                          AND table_definition.relname = 'doanh_thu'
                          AND constraint_definition.contype = 'c'
                          AND pg_get_constraintdef(constraint_definition.oid) ~ 'so_tien.*>=.*0'
                    )
                    """));

            assertTrue(queryBoolean(connection, """
                    SELECT EXISTS (
                        SELECT 1
                        FROM pg_constraint constraint_definition
                        JOIN pg_class table_definition ON table_definition.oid = constraint_definition.conrelid
                        JOIN pg_namespace schema_definition ON schema_definition.oid = table_definition.relnamespace
                        WHERE schema_definition.nspname = current_schema()
                          AND table_definition.relname = 'patients'
                          AND constraint_definition.conname = 'chk_patients_gioi_tinh'
                    )
                    """));

            assertFalse(queryBoolean(connection, """
                    SELECT EXISTS (
                        SELECT 1
                        FROM information_schema.columns
                        WHERE table_schema = current_schema()
                          AND table_name = 'doanh_thu'
                          AND column_name IN ('tong_cong', 'thue_gtgt', 'thue_tncn')
                    )
                    """));

            assertEquals(
                    Set.of(
                            "idx_patients_ho_va_ten",
                            "idx_patients_ho_va_ten_search",
                            "idx_patients_so_dien_thoai",
                            "idx_patients_active_search",
                            "idx_patients_active_phone_search",
                            "idx_patients_active_id",
                            "idx_patients_deleted_at",
                            "idx_visits_patient_id",
                            "idx_visits_created_at",
                            "idx_visits_ngay_kham",
                            "idx_doanh_thu_visit_id",
                            "idx_doanh_thu_ngay_thang"
                    ),
                    querySet(connection, """
                            SELECT indexname
                            FROM pg_indexes
                            WHERE schemaname = current_schema()
                              AND indexname IN (
                                  'idx_patients_ho_va_ten',
                                  'idx_patients_ho_va_ten_search',
                                  'idx_patients_so_dien_thoai',
                                  'idx_patients_active_search',
                                  'idx_patients_active_phone_search',
                                  'idx_patients_active_id',
                                  'idx_patients_deleted_at',
                                  'idx_visits_patient_id',
                                  'idx_visits_created_at',
                                  'idx_visits_ngay_kham',
                                  'idx_doanh_thu_visit_id',
                                  'idx_doanh_thu_ngay_thang'
                              )
                            """)
            );
        }
    }

    private static void assertColumnNullability(Connection connection) throws SQLException {
        Map<String, String> expectedNullability = Map.ofEntries(
                Map.entry("patients.ho_va_ten", "NO"),
                Map.entry("patients.gioi_tinh", "NO"),
                Map.entry("patients.ngay_sinh", "NO"),
                Map.entry("patients.so_dien_thoai", "YES"),
                Map.entry("patients.deleted_at", "YES"),
                Map.entry("patients.giay_to_tuy_than", "YES"),
                Map.entry("patients.so_the_bhyt", "YES"),
                Map.entry("patients.dia_chi", "YES"),
                Map.entry("patients.nghe_nghiep", "YES"),
                Map.entry("patients.dan_toc", "YES"),
                Map.entry("visits.patient_id", "NO"),
                Map.entry("visits.tt", "NO"),
                Map.entry("visits.ngay_kham", "NO"),
                Map.entry("visits.trieu_chung", "NO"),
                Map.entry("visits.chan_doan", "NO"),
                Map.entry("visits.phuong_phap_dieu_tri", "NO"),
                Map.entry("visits.bac_si_kham", "NO"),
                Map.entry("visits.ghi_chu", "YES"),
                Map.entry("doanh_thu.visit_id", "NO"),
                Map.entry("doanh_thu.ngay_thang", "NO"),
                Map.entry("doanh_thu.dien_giai", "YES"),
                Map.entry("doanh_thu.so_tien", "NO"),
                Map.entry("doanh_thu.so_hieu", "YES")
        );

        try (var statement = connection.prepareStatement("""
                SELECT table_name, column_name, is_nullable
                FROM information_schema.columns
                WHERE table_schema = current_schema()
                  AND table_name IN ('patients', 'visits', 'doanh_thu')
                """); var results = statement.executeQuery()) {
            int checkedColumns = 0;
            while (results.next()) {
                String column = results.getString("table_name") + "." + results.getString("column_name");
                if (expectedNullability.containsKey(column)) {
                    assertEquals(expectedNullability.get(column), results.getString("is_nullable"), column);
                    checkedColumns++;
                }
            }
            assertEquals(expectedNullability.size(), checkedColumns, "All required columns must exist.");
        }
    }

    private static Set<String> querySet(Connection connection, String sql) throws SQLException {
        try (var statement = connection.prepareStatement(sql); var results = statement.executeQuery()) {
            var values = new java.util.HashSet<String>();
            while (results.next()) {
                values.add(results.getString(1));
            }
            return values;
        }
    }

    private static boolean queryBoolean(Connection connection, String sql) throws SQLException {
        try (var statement = connection.prepareStatement(sql); ResultSet results = statement.executeQuery()) {
            assertTrue(results.next());
            return results.getBoolean(1);
        }
    }
}
