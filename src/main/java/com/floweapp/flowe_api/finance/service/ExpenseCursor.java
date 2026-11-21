package com.floweapp.flowe_api.finance.service;

import com.floweapp.flowe_api.task.exception.InvalidQueryParameterException;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

public final class ExpenseCursor {
    private ExpenseCursor() {}

    public record Decoded(OffsetDateTime spentAt, UUID id) {}

    public static String encode(OffsetDateTime spentAt, UUID id) {
        String raw = spentAt.toString() + "|" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static Decoded decode(String cursor) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int sep = raw.indexOf('|');
            if (sep < 0) throw new IllegalArgumentException("Отсутствует разделитель");
            return new Decoded(
                    OffsetDateTime.parse(raw.substring(0, sep)),
                    UUID.fromString(raw.substring(sep + 1))
            );
        } catch (Exception e) {
            throw new InvalidQueryParameterException("Неправильный формат курсора");
        }
    }
}
