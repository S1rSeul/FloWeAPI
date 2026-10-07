package com.floweapp.flowe_api.task.service;

import com.floweapp.flowe_api.task.exception.InvalidQueryParameterException;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

public final class TaskCursor {

    private TaskCursor() {}

    public record Decoded(OffsetDateTime value, UUID id) {}

    public static String encode(OffsetDateTime value, UUID id) {
        String raw = (value == null ? "null" : value.toString()) + "|" + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static Decoded decode(String cursor) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int sep = raw.indexOf('|');
            if (sep < 0) throw new IllegalArgumentException("Отсутствует разделитель");
            String valuePart = raw.substring(0, sep);
            String idPart = raw.substring(sep + 1);
            OffsetDateTime value = valuePart.equals("null") ? null : OffsetDateTime.parse(valuePart);
            UUID id = UUID.fromString(idPart);

            return new Decoded(value, id);
        } catch (Exception e) {
            throw new InvalidQueryParameterException("Недопустимый курсор");
        }
    }
}
