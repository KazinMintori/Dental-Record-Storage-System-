package com.dentalclinic.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DatePickerSupportTest {

    @Test
    void formatsAndParsesDayMonthYear() {
        LocalDate date = LocalDate.of(1995, 3, 12);
        assertEquals("12/03/1995", DatePickerSupport.format(date));
        assertEquals(date, DatePickerSupport.parse("12/03/1995"));
    }

    @Test
    void acceptsSingleDigitDayAndMonthThenFormatsWithLeadingZeroes() {
        LocalDate date = DatePickerSupport.parse("1/1/2001");
        assertEquals(LocalDate.of(2001, 1, 1), date);
        assertEquals("01/01/2001", DatePickerSupport.format(date));
    }

    @Test
    void parsingIsStrict() {
        assertThrows(DateTimeParseException.class, () -> DatePickerSupport.parse("31/02/2026"));
        assertThrows(DateTimeParseException.class, () -> DatePickerSupport.parse("2026-08-14"));
    }
}
