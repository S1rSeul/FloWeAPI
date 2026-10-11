package com.floweapp.flowe_api.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

@Schema(
        description = """
                Запрос на частичное обновление задачи.

                Все поля необязательны. Отсутствующее поле остаётся без изменений.
                Передай null в description или dueDate, чтобы очистить соответствующее значение.
                Статус задачи изменяется отдельным endpoint /api/v1/tasks/{id}/status.
                """
)
public record UpdateTaskRequestDto(

        @Schema(
                description = """
                        Новое название задачи.
                        После trim длина должна быть от 1 до 100 символов.
                        Если поле отсутствует или равно null, название не изменяется.
                        """,
                example = "Купить продукты к ужину",
                nullable = true
        )
        JsonNode title,

        @Schema(
                description = """
                        Новое описание задачи, не более 2000 символов.
                        Передай null, чтобы удалить описание.
                        """,
                example = "Молоко, хлеб, сыр и фрукты",
                nullable = true
        )
        JsonNode description,

        @Schema(
                description = """
                        Назначить или снять назначение задачи с текущего пользователя.
                        Если поле отсутствует, значение не изменяется.
                        """,
                example = "true",
                nullable = true
        )
        JsonNode assignedToMe,

        @Schema(
                description = """
                        Назначить или снять назначение задачи с партнёра.
                        Если поле отсутствует, значение не изменяется.
                        """,
                example = "false",
                nullable = true
        )
        JsonNode assignedToPartner,

        @Schema(
                description = """
                        Новый дедлайн в ISO-8601 с timezone offset.
                        Значение должно быть в будущем. Передай null, чтобы удалить дедлайн.
                        """,
                example = "2026-10-16T19:00:00+04:00",
                format = "date-time",
                nullable = true
        )
        JsonNode dueDate
) {}