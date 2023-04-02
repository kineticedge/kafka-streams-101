package io.kineticedge.ks101.admin;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateTimeConverter extends BaseConverter<ZonedDateTime> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    public DateTimeConverter(String optionName) {
        super(optionName);
    }

    public ZonedDateTime convert(String value) {
        try {
            return ZonedDateTime.parse(value, FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ParameterException(this.getErrorString(value, String.format("an ISO-8601 formatted date (yyyy-MM-dd HH:mm:ss z)")));
        }
    }
}