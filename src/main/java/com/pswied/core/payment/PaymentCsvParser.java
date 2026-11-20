package com.pswied.core.payment;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class PaymentCsvParser {

    public static final int MAX_CONTENT_LENGTH = 5 * 1024 * 1024;
    private static final Set<String> REQUIRED_HEADERS = Set.of(
            "source",
            "transactionReference",
            "endToEndId",
            "amount",
            "currency",
            "counterparty",
            "valueDate",
            "status");

    public List<PaymentRecordRequest> parse(String content) {
        if (content == null || content.isBlank()) {
            throw new PaymentImportValidationException(List.of("CSV content must not be empty."));
        }
        if (content.getBytes(StandardCharsets.UTF_8).length > MAX_CONTENT_LENGTH) {
            throw new PaymentImportValidationException(List.of("CSV content exceeds the 5 MiB limit."));
        }

        try (CSVParser parser = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .get()
                .parse(new StringReader(content))) {
            validateHeaders(parser.getHeaderMap());
            List<PaymentRecordRequest> requests = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            for (CSVRecord record : parser) {
                if (requests.size() >= PaymentImportService.MAX_RECORDS) {
                    throw new PaymentImportValidationException(List.of(
                            "An import may contain no more than %d records."
                                    .formatted(PaymentImportService.MAX_RECORDS)));
                }
                long csvRowNumber = record.getRecordNumber() + 1;
                try {
                    requests.add(toRequest(record));
                } catch (IllegalArgumentException exception) {
                    errors.add("CSV row %d: %s".formatted(csvRowNumber, exception.getMessage()));
                }
            }
            if (!errors.isEmpty()) {
                throw new PaymentImportValidationException(errors);
            }
            if (requests.isEmpty()) {
                throw new PaymentImportValidationException(List.of("CSV must contain at least one payment record."));
            }
            return List.copyOf(requests);
        } catch (IOException exception) {
            throw new PaymentImportValidationException(List.of("CSV content could not be parsed."));
        }
    }

    private void validateHeaders(Map<String, Integer> headers) {
        if (headers == null || !headers.keySet().equals(REQUIRED_HEADERS)) {
            throw new PaymentImportValidationException(List.of(
                    "CSV headers must be exactly: source,transactionReference,endToEndId,amount,currency,"
                            + "counterparty,valueDate,status."));
        }
    }

    private PaymentRecordRequest toRequest(CSVRecord record) {
        String source = required(record, "source");
        String status = required(record, "status");
        return new PaymentRecordRequest(
                enumValue(PaymentSource.class, source, "source"),
                required(record, "transactionReference"),
                optional(record, "endToEndId"),
                decimal(required(record, "amount")),
                required(record, "currency"),
                required(record, "counterparty"),
                date(required(record, "valueDate")),
                enumValue(PaymentStatus.class, status, "status"));
    }

    private String required(CSVRecord record, String column) {
        String value = record.get(column);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("column '%s' is required.".formatted(column));
        }
        return value;
    }

    private String optional(CSVRecord record, String column) {
        String value = record.get(column);
        return value == null || value.isBlank() ? null : value;
    }

    private BigDecimal decimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("column 'amount' must be a decimal number.");
        }
    }

    private LocalDate date(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("column 'valueDate' must use ISO date format YYYY-MM-DD.");
        }
    }

    private <T extends Enum<T>> T enumValue(Class<T> type, String value, String column) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "column '%s' must be one of %s.".formatted(column, String.join(", ", enumNames(type))));
        }
    }

    private <T extends Enum<T>> List<String> enumNames(Class<T> type) {
        return java.util.Arrays.stream(type.getEnumConstants())
                .map(Enum::name)
                .toList();
    }
}
