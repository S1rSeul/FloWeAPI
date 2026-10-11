package com.floweapp.flowe_api.task.controller;

import com.floweapp.flowe_api.common.ErrorResponse;
import com.floweapp.flowe_api.common.dto.CursorPageResponseDto;
import com.floweapp.flowe_api.task.dto.CreateTaskRequestDto;
import com.floweapp.flowe_api.task.dto.TaskResponseDto;
import com.floweapp.flowe_api.task.dto.UpdateTaskRequestDto;
import com.floweapp.flowe_api.task.dto.UpdateTaskStatusRequestDto;
import com.floweapp.flowe_api.task.service.TaskService;
import com.floweapp.flowe_api.user.entity.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(
        name = "Tasks",
        description = """
                Управление общими задачами активного пространства.
                Все endpoints требуют access token в заголовке Authorization: Bearer <token>.
                """
)
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @Operation(
            summary = "Создать задачу",
            description = """
                    Создаёт новую задачу в активном пространстве текущего пользователя.
                    Новая задача получает статус `todo`.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Задача успешно создана",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TaskResponseDto.class),
                            examples = @ExampleObject(
                                    name = "Созданная задача",
                                    value = """
                                            {
                                              "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                              "title": "Купить продукты",
                                              "description": "Молоко, хлеб, сыр",
                                              "createdById": "550e8400-e29b-41d4-a716-446655440000",
                                              "assignedToMe": true,
                                              "assignedToPartner": false,
                                              "dueDate": "2026-10-15T18:00:00+04:00",
                                              "status": "todo",
                                              "isOverdue": false,
                                              "completedAt": null,
                                              "createdAt": "2026-10-11T21:23:00+04:00",
                                              "updatedAt": "2026-10-11T21:23:00+04:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Невалидные данные запроса",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Пустое название",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-11T21:23:00",
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "title: Требуется название",
                                                      "path": "/api/v1/tasks"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Слишком длинное название",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-11T21:23:00",
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "title: Название должно быть от 1 до 100 символов",
                                                      "path": "/api/v1/tasks"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Дедлайн в прошлом",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-11T21:23:00",
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "dueDate: Дата дедлайна должна быть в будущем времени",
                                                      "path": "/api/v1/tasks"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Некорректный JSON",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-11T21:23:00",
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "Искаженный JSON-файл или недопустимое значение",
                                                      "path": "/api/v1/tasks"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access token отсутствует или недействителен",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Требуется аутентификация",
                                    value = """
                                            {
                                              "timestamp": "2026-10-11T21:23:00",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Требуется аутентификация",
                                              "path": "/api/v1/tasks"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не состоит в пространстве",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Пространство не найдено",
                                    value = """
                                            {
                                              "timestamp": "2026-10-11T21:23:00",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "Пара пользователя не найдена",
                                              "path": "/api/v1/tasks"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Пространство ещё не активно",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Пространство не активно",
                                    value = """
                                            {
                                              "timestamp": "2026-10-11T21:23:00",
                                              "status": 409,
                                              "error": "Conflict",
                                              "message": "Пространство ещё не активно",
                                              "path": "/api/v1/tasks"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<TaskResponseDto> create(
            @Parameter(hidden = true)
            @AuthenticationPrincipal User currentUser,

            @Valid @RequestBody CreateTaskRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.createTask(currentUser, request));
    }

    @Operation(
            summary = "Получить список задач",
            description = """
                    Возвращает задачи активного пространства с курсорной пагинацией.

                    Допустимые значения `sort`: `createdAt`, `dueAt`.
                    Допустимые значения `order`: `asc`, `desc`.
                    Значение `limit` должно находиться в диапазоне от 1 до 100; по умолчанию — 20.

                    Для следующей страницы передай `cursor` из поля `nextCursor` предыдущего ответа,
                    сохраняя те же `sort` и `order`.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Страница задач",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CursorPageResponseDto.class),
                            examples = @ExampleObject(
                                    name = "Первая страница задач",
                                    value = """
                                            {
                                              "items": [
                                                {
                                                  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                                  "title": "Купить продукты",
                                                  "description": "Молоко, хлеб, сыр",
                                                  "createdById": "550e8400-e29b-41d4-a716-446655440000",
                                                  "assignedToMe": true,
                                                  "assignedToPartner": false,
                                                  "dueDate": "2026-10-15T18:00:00+04:00",
                                                  "status": "todo",
                                                  "isOverdue": false,
                                                  "completedAt": null,
                                                  "createdAt": "2026-10-11T21:23:00+04:00",
                                                  "updatedAt": "2026-10-11T21:23:00+04:00"
                                                }
                                              ],
                                              "nextCursor": "MjAyNi0xMC0xMVQyMToyMzowMCswNDowMHwzZmE4NWY2NC01NzE3LTQ1NjItYjNmYy0yYzk2M2Y2NmFmYTY",
                                              "hasMore": true
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Недопустимый query-параметр",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Неверная сортировка",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-11T21:23:00",
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "sort должен быть 'createdAt' или 'dueAt'",
                                                      "path": "/api/v1/tasks"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Неверное направление",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-11T21:23:00",
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "order должен быть 'asc' или 'desc'",
                                                      "path": "/api/v1/tasks"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Недопустимый limit",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-11T21:23:00",
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "limit должен быть от 1 до 100",
                                                      "path": "/api/v1/tasks"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Недопустимый cursor",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-11T21:23:00",
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "Недопустимый курсор",
                                                      "path": "/api/v1/tasks"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access token отсутствует или недействителен",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Требуется аутентификация",
                                    value = """
                                            {
                                              "timestamp": "2026-10-11T21:23:00",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Требуется аутентификация",
                                              "path": "/api/v1/tasks"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не состоит в пространстве",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Пространство не найдено",
                                    value = """
                                            {
                                              "timestamp": "2026-10-11T21:23:00",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "Пара пользователя не найдена",
                                              "path": "/api/v1/tasks"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Пространство ещё не активно",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Пространство не активно",
                                    value = """
                                            {
                                              "timestamp": "2026-10-11T21:23:00",
                                              "status": 409,
                                              "error": "Conflict",
                                              "message": "Пространство ещё не активно",
                                              "path": "/api/v1/tasks"
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CursorPageResponseDto<TaskResponseDto>> list(
            @Parameter(hidden = true)
            @AuthenticationPrincipal User currentUser,

            @Parameter(
                    description = "Поле сортировки: createdAt или dueAt",
                    example = "createdAt"
            )
            @RequestParam(defaultValue = "createdAt") String sort,

            @Parameter(
                    description = "Направление сортировки: asc или desc",
                    example = "desc"
            )
            @RequestParam(defaultValue = "desc") String order,

            @Parameter(
                    description = "Число задач на странице: от 1 до 100; по умолчанию — 20",
                    example = "20"
            )
            @RequestParam(required = false) Integer limit,

            @Parameter(
                    description = "Курсор следующей страницы из поля nextCursor предыдущего ответа"
            )
            @RequestParam(required = false) String cursor
    ) {
        return ResponseEntity.ok(
                taskService.listTasks(currentUser, sort, order, limit, cursor)
        );
    }

    @Operation(
            summary = "Получить задачу по ID",
            description = "Возвращает задачу из активного пространства текущего пользователя."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Задача найдена",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TaskResponseDto.class),
                            examples = @ExampleObject(
                                    name = "Задача",
                                    value = """
                                            {
                                              "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                              "title": "Купить продукты",
                                              "description": "Молоко, хлеб, сыр",
                                              "createdById": "550e8400-e29b-41d4-a716-446655440000",
                                              "assignedToMe": true,
                                              "assignedToPartner": false,
                                              "dueDate": "2026-10-15T18:00:00+04:00",
                                              "status": "todo",
                                              "isOverdue": false,
                                              "completedAt": null,
                                              "createdAt": "2026-10-11T21:23:00+04:00",
                                              "updatedAt": "2026-10-11T21:23:00+04:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access token отсутствует или недействителен",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Требуется аутентификация",
                                    value = """
                                            {
                                              "timestamp": "2026-10-11T21:23:00",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Требуется аутентификация",
                                              "path": "/api/v1/tasks/3fa85f64-5717-4562-b3fc-2c963f66afa6"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Задача или пространство не найдены",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Задача не найдена",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-11T21:23:00",
                                                      "status": 404,
                                                      "error": "Not Found",
                                                      "message": "Задача не найдена",
                                                      "path": "/api/v1/tasks/3fa85f64-5717-4562-b3fc-2c963f66afa6"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Пространство не найдено",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-11T21:23:00",
                                                      "status": 404,
                                                      "error": "Not Found",
                                                      "message": "Пара пользователя не найдена",
                                                      "path": "/api/v1/tasks/3fa85f64-5717-4562-b3fc-2c963f66afa6"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Пространство ещё не активно",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Пространство не активно",
                                    value = """
                                            {
                                              "timestamp": "2026-10-11T21:23:00",
                                              "status": 409,
                                              "error": "Conflict",
                                              "message": "Пространство ещё не активно",
                                              "path": "/api/v1/tasks/3fa85f64-5717-4562-b3fc-2c963f66afa6"
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TaskResponseDto> getById(
            @Parameter(hidden = true)
            @AuthenticationPrincipal User currentUser,

            @Parameter(
                    description = "Идентификатор задачи",
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
            )
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(taskService.getTask(currentUser, id));
    }

    @Operation(
            summary = "Частично обновить задачу",
            description = """
                    Частично обновляет задачу. Все поля тела необязательны.

                    Если передать `description: null`, описание будет удалено.
                    Если передать `dueDate: null`, дедлайн будет удалён.
                    Статус меняется отдельным endpoint: `PATCH /api/v1/tasks/{id}/status`.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Задача обновлена",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TaskResponseDto.class),
                            examples = @ExampleObject(
                                    name = "Обновлённая задача",
                                    value = """
                                            {
                                              "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                              "title": "Купить продукты к ужину",
                                              "description": "Молоко, хлеб, сыр",
                                              "createdById": "550e8400-e29b-41d4-a716-446655440000",
                                              "assignedToMe": true,
                                              "assignedToPartner": true,
                                              "dueDate": "2026-10-16T19:00:00+04:00",
                                              "status": "todo",
                                              "isOverdue": false,
                                              "completedAt": null,
                                              "createdAt": "2026-10-11T21:23:00+04:00",
                                              "updatedAt": "2026-10-11T21:25:00+04:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные обновления",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Невалидное название",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-11T21:23:00",
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "Название должно быть от 1 до 100 символов",
                                                      "path": "/api/v1/tasks/3fa85f64-5717-4562-b3fc-2c963f66afa6"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Слишком длинное описание",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-11T21:23:00",
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "Размер описания не должен превышать 2000 символов",
                                                      "path": "/api/v1/tasks/3fa85f64-5717-4562-b3fc-2c963f66afa6"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Дедлайн в прошлом",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-11T21:23:00",
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "Дата дедлайна должна быть в будущем времени",
                                                      "path": "/api/v1/tasks/3fa85f64-5717-4562-b3fc-2c963f66afa6"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Некорректный JSON",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-11T21:23:00",
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "Искаженный JSON-файл или недопустимое значение",
                                                      "path": "/api/v1/tasks/3fa85f64-5717-4562-b3fc-2c963f66afa6"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access token отсутствует или недействителен",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Требуется аутентификация",
                                    value = """
                                            {
                                              "timestamp": "2026-10-11T21:23:00",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Требуется аутентификация",
                                              "path": "/api/v1/tasks/3fa85f64-5717-4562-b3fc-2c963f66afa6"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Задача или пространство не найдены",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Задача не найдена",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-11T21:23:00",
                                                      "status": 404,
                                                      "error": "Not Found",
                                                      "message": "Задача не найдена",
                                                      "path": "/api/v1/tasks/3fa85f64-5717-4562-b3fc-2c963f66afa6"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Пространство не найдено",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-11T21:23:00",
                                                      "status": 404,
                                                      "error": "Not Found",
                                                      "message": "Пара пользователя не найдена",
                                                      "path": "/api/v1/tasks/3fa85f64-5717-4562-b3fc-2c963f66afa6"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Пространство ещё не активно",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Пространство не активно",
                                    value = """
                                            {
                                              "timestamp": "2026-10-11T21:23:00",
                                              "status": 409,
                                              "error": "Conflict",
                                              "message": "Пространство ещё не активно",
                                              "path": "/api/v1/tasks/3fa85f64-5717-4562-b3fc-2c963f66afa6"
                                            }
                                            """
                            )
                    )
            )
    })
    @PatchMapping(
            value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<TaskResponseDto> update(
            @Parameter(hidden = true)
            @AuthenticationPrincipal User currentUser,

            @Parameter(
                    description = "Идентификатор задачи",
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
            )
            @PathVariable UUID id,

            @Valid @RequestBody UpdateTaskRequestDto request
    ) {
        return ResponseEntity.ok(taskService.updateTask(currentUser, id, request));
    }

    @Operation(
            summary = "Удалить задачу",
            description = "Удаляет задачу из активного пространства. При успехе тело ответа отсутствует."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Задача удалена"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access token отсутствует или недействителен",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Требуется аутентификация",
                                    value = """
                                            {
                                              "timestamp": "2026-10-11T21:23:00",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Требуется аутентификация",
                                              "path": "/api/v1/tasks/3fa85f64-5717-4562-b3fc-2c963f66afa6"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Задача или пространство не найдены",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Задача не найдена",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-11T21:23:00",
                                                      "status": 404,
                                                      "error": "Not Found",
                                                      "message": "Задача не найдена",
                                                      "path": "/api/v1/tasks/3fa85f64-5717-4562-b3fc-2c963f66afa6"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Пространство не найдено",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-11T21:23:00",
                                                      "status": 404,
                                                      "error": "Not Found",
                                                      "message": "Пара пользователя не найдена",
                                                      "path": "/api/v1/tasks/3fa85f64-5717-4562-b3fc-2c963f66afa6"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Пространство ещё не активно",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Пространство не активно",
                                    value = """
                                            {
                                              "timestamp": "2026-10-11T21:23:00",
                                              "status": 409,
                                              "error": "Conflict",
                                              "message": "Пространство ещё не активно",
                                              "path": "/api/v1/tasks/3fa85f64-5717-4562-b3fc-2c963f66afa6"
                                            }
                                            """
                            )
                    )
            )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(hidden = true)
            @AuthenticationPrincipal User currentUser,

            @Parameter(
                    description = "Идентификатор задачи",
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
            )
            @PathVariable UUID id
    ) {
        taskService.deleteTask(currentUser, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Изменить статус задачи",
            description = """
                    Изменяет статус задачи. Допустимые статусы:
                    `todo`, `in_progress`, `done`, `closed`.

                    Допустимые переходы:
                    - `todo` → `in_progress`, `done`, `closed`
                    - `in_progress` → `todo`, `done`, `closed`
                    - `done` → `todo`, `in_progress`
                    - `closed` → `todo`

                    Повторная установка текущего статуса возвращает задачу без изменений.
                    При переходе в `done` сервер устанавливает `completedAt`.
                    При выходе из `done` поле `completedAt` становится `null`.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Статус обновлён",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TaskResponseDto.class),
                            examples = @ExampleObject(
                                    name = "Завершённая задача",
                                    value = """
                                            {
                                              "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                              "title": "Купить продукты",
                                              "description": "Молоко, хлеб, сыр",
                                              "createdById": "550e8400-e29b-41d4-a716-446655440000",
                                              "assignedToMe": true,
                                              "assignedToPartner": false,
                                              "dueDate": "2026-10-15T18:00:00+04:00",
                                              "status": "done",
                                              "isOverdue": false,
                                              "completedAt": "2026-10-12T10:30:00+04:00",
                                              "createdAt": "2026-10-11T21:23:00+04:00",
                                              "updatedAt": "2026-10-12T10:30:00+04:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Невалидный запрос",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Статус не передан",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-11T21:23:00",
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "status: Требуется статус",
                                                      "path": "/api/v1/tasks/3fa85f64-5717-4562-b3fc-2c963f66afa6/status"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Неизвестный статус",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-11T21:23:00",
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "Искаженный JSON-файл или недопустимое значение",
                                                      "path": "/api/v1/tasks/3fa85f64-5717-4562-b3fc-2c963f66afa6/status"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access token отсутствует или недействителен",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Требуется аутентификация",
                                    value = """
                                            {
                                              "timestamp": "2026-10-11T21:23:00",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Требуется аутентификация",
                                              "path": "/api/v1/tasks/3fa85f64-5717-4562-b3fc-2c963f66afa6/status"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Задача или пространство не найдены",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Задача не найдена",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-11T21:23:00",
                                                      "status": 404,
                                                      "error": "Not Found",
                                                      "message": "Задача не найдена",
                                                      "path": "/api/v1/tasks/3fa85f64-5717-4562-b3fc-2c963f66afa6/status"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Пространство не найдено",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-11T21:23:00",
                                                      "status": 404,
                                                      "error": "Not Found",
                                                      "message": "Пара пользователя не найдена",
                                                      "path": "/api/v1/tasks/3fa85f64-5717-4562-b3fc-2c963f66afa6/status"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Недопустимый переход статуса или неактивное пространство",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Недопустимый переход",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-11T21:23:00",
                                                      "status": 409,
                                                      "error": "Conflict",
                                                      "message": "Невозможно сменить статус задачи с 'done' на 'closed'",
                                                      "path": "/api/v1/tasks/3fa85f64-5717-4562-b3fc-2c963f66afa6/status"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Пространство не активно",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-11T21:23:00",
                                                      "status": 409,
                                                      "error": "Conflict",
                                                      "message": "Пространство ещё не активно",
                                                      "path": "/api/v1/tasks/3fa85f64-5717-4562-b3fc-2c963f66afa6/status"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    @PatchMapping(
            value = "/{id}/status",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<TaskResponseDto> updateStatus(
            @Parameter(hidden = true)
            @AuthenticationPrincipal User currentUser,

            @Parameter(
                    description = "Идентификатор задачи",
                    example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
            )
            @PathVariable UUID id,

            @Valid @RequestBody UpdateTaskStatusRequestDto request
    ) {
        return ResponseEntity.ok(taskService.updateStatus(currentUser, id, request));
    }
}