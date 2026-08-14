package com.dentalclinic.model.report;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenueReportRow(
        String referenceNumber,
        LocalDate documentDate,
        String description,
        BigDecimal amount
) {
}
