package com.dentalclinic.util;

import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;

public final class DatePickerSupport {

    public static final String DATE_PATTERN = "dd/MM/yyyy";
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter
            .ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter INPUT_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
            .appendLiteral('/')
            .appendValue(ChronoField.MONTH_OF_YEAR, 1, 2, SignStyle.NOT_NEGATIVE)
            .appendLiteral('/')
            .appendValue(ChronoField.YEAR, 4)
            .toFormatter()
            .withResolverStyle(ResolverStyle.STRICT);

    private DatePickerSupport() {
    }

    public static void configure(DatePicker datePicker) {
        datePicker.setPromptText("Ngày/Tháng/Năm");
        StringConverter<LocalDate> converter = new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return format(date);
            }

            @Override
            public LocalDate fromString(String value) {
                if (value == null || value.isBlank()) {
                    return null;
                }
                return parse(value);
            }
        };
        datePicker.setConverter(converter);
        datePicker.valueProperty().addListener((observable, previous, current) ->
                datePicker.getEditor().setText(converter.toString(current)));
        datePicker.getEditor().focusedProperty().addListener((observable, hadFocus, hasFocus) -> {
            if (!hasFocus) {
                normalizeEditor(datePicker);
            }
        });
        datePicker.getEditor().setText(converter.toString(datePicker.getValue()));
    }

    public static String format(LocalDate date) {
        return date == null ? "" : DISPLAY_FORMATTER.format(date);
    }

    public static LocalDate parse(String value) {
        return LocalDate.parse(value.trim(), INPUT_FORMATTER);
    }

    public static LocalDate commit(DatePicker datePicker) {
        String value = datePicker.getEditor().getText();
        if (value == null || value.isBlank()) {
            datePicker.setValue(null);
            datePicker.getEditor().clear();
            return null;
        }
        LocalDate date = parse(value);
        datePicker.setValue(date);
        datePicker.getEditor().setText(format(date));
        return date;
    }

    private static void normalizeEditor(DatePicker datePicker) {
        try {
            commit(datePicker);
        } catch (DateTimeParseException ignored) {
            // Keep invalid input intact so the owning form can show its contextual validation message.
        }
    }
}
