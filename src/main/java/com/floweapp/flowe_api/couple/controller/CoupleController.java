package com.floweapp.flowe_api.couple.controller;

import com.floweapp.flowe_api.common.ErrorResponse;
import com.floweapp.flowe_api.couple.dto.CoupleMemberResponseDto;
import com.floweapp.flowe_api.couple.dto.CoupleResponseDto;
import com.floweapp.flowe_api.couple.dto.CoupleNameRequestDto;
import com.floweapp.flowe_api.couple.dto.JoinCoupleRequestDto;
import com.floweapp.flowe_api.couple.service.CoupleService;
import com.floweapp.flowe_api.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "Couple",
        description = "Создание, переименование и присоединение к пространству"
)
@RestController
@RequestMapping("/api/v1/couples")
@RequiredArgsConstructor
public class CoupleController {

    private final CoupleService coupleService;

    @Operation(
            summary = "Создать новое пространство",
            description = """
                    Создаёт пространство в статусе `pending`, делает текущего пользователя его владельцем
                    и возвращает invite-код для партнёра. Один пользователь может состоять только в одном пространстве
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Пространство создано",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CoupleResponseDto.class),
                            examples = @ExampleObject(
                                    name = "Новое пространство",
                                    value = """
                                            {
                                              "id": "550e8400-e29b-41d4-a716-446655440000",
                                              "name": "Наше пространство",
                                              "status": "pending",
                                              "partnerName": null,
                                              "inviteCode": "A1B2C3D4E5"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Невалидное название пространства",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Пустое название",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-08T08:10:00",
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "name: Название должно быть от 1 до 100 символов; name: Название пространства не может быть пустым",
                                                      "path": "/api/v1/couples"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Слишком длинное название",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-08T08:10:00",
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "name: Название должно быть от 1 до 100 символов",
                                                      "path": "/api/v1/couples"
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
                                    name = "Нет аутентификации",
                                    value = """
                                            {
                                              "timestamp": "2026-10-08T08:10:00",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Требуется аутентификация",
                                              "path": "/api/v1/couples"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Пользователь уже состоит в пространстве",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Уже в паре",
                                    value = """
                                            {
                                              "timestamp": "2026-10-08T08:10:00",
                                              "status": 409,
                                              "error": "Conflict",
                                              "message": "Пользователь уже находится в паре",
                                              "path": "/api/v1/couples"
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
    public ResponseEntity<CoupleResponseDto> create(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CoupleNameRequestDto request
            ) {
        CoupleResponseDto response = coupleService.createCouple(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @Operation(
            summary = "Получить своё пространство",
            description = """
                    Возвращает пространство текущего пользователя. Пока статус `pending`, в ответе есть `inviteCode`;
                    после присоединения партнёра статус становится `active`, появляется `partnerName`, а `inviteCode` равен `null`
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Данные пространства",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CoupleResponseDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Ожидает партнёра",
                                            value = """
                                                    {
                                                      "id": "550e8400-e29b-41d4-a716-446655440000",
                                                      "name": "Наше пространство",
                                                      "status": "pending",
                                                      "partnerName": null,
                                                      "inviteCode": "A1B2C3D4E5"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Активное пространство",
                                            value = """
                                                    {
                                                      "id": "550e8400-e29b-41d4-a716-446655440000",
                                                      "name": "Наше пространство",
                                                      "status": "active",
                                                      "partnerName": "Malvina",
                                                      "inviteCode": null
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
                                    name = "Нет аутентификации",
                                    value = """
                                            {
                                              "timestamp": "2026-10-08T08:10:00",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Требуется аутентификация",
                                              "path": "/api/v1/couples/me"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "У пользователя нет пространства",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Пространство не найдено",
                                    value = """
                                            {
                                              "timestamp": "2026-10-08T08:10:00",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "Пара пользователя не найдена",
                                              "path": "/api/v1/couples/me"
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CoupleResponseDto> getMyCouple(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(coupleService.getMyCouple(currentUser));
    }

    @Operation(
            summary = "Переименовать своё пространство",
            description = "Изменяет название пространства и возвращает обновлённые данные"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Пространство переименовано",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CoupleResponseDto.class),
                            examples = @ExampleObject(
                                    name = "Обновлённое пространство",
                                    value = """
                                            {
                                              "id": "550e8400-e29b-41d4-a716-446655440000",
                                              "name": "Наше пространство",
                                              "status": "active",
                                              "partnerName": "Malvina",
                                              "inviteCode": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Невалидное название пространства",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Пустое название",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-08T08:10:00",
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "name: Требуется название пространства",
                                                      "path": "/api/v1/couples/me"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Слишком длинное название",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-08T08:10:00",
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "name: Имя должно быть от 1 до 100 символов",
                                                      "path": "/api/v1/couples/me"
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
                                    name = "Нет аутентификации",
                                    value = """
                                            {
                                              "timestamp": "2026-10-08T08:10:00",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Требуется аутентификация",
                                              "path": "/api/v1/couples/me"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "У пользователя нет пространства",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Пространство не найдено",
                                    value = """
                                            {
                                              "timestamp": "2026-10-08T08:10:00",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "Пользователь не состоит в паре",
                                              "path": "/api/v1/couples/me"
                                            }
                                            """
                            )
                    )
            )
    })
    @PatchMapping(
            value = "/me",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<CoupleResponseDto> updateMyCouple(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CoupleNameRequestDto request
    ) {
        return ResponseEntity.ok(coupleService.updateMyCouple(currentUser, request));
    }

    @Operation(
            summary = "Присоединиться к пространству",
            description = """
                    Присоединяет текущего пользователя к пространству по 10-символьному invite-коду.
                    Код нечувствителен к регистру и пробелам по краям. После успешного присоединения
                    пространство становится `active`, а код удаляется
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Присоединение выполнено",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CoupleResponseDto.class),
                            examples = @ExampleObject(
                                    name = "Активное пространство",
                                    value = """
                                            {
                                              "id": "550e8400-e29b-41d4-a716-446655440000",
                                              "name": "Наше пространство",
                                              "status": "active",
                                              "partnerName": "Malvina",
                                              "inviteCode": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Невалидный invite-код",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Неверная длина кода",
                                    value = """
                                            {
                                              "timestamp": "2026-10-08T08:10:00",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "inviteCode: Invite код не может быть пустым; inviteCode: Invite-код должен быть ровно из 10 символов",
                                              "path": "/api/v1/couples/join"
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
                                    name = "Нет аутентификации",
                                    value = """
                                            {
                                              "timestamp": "2026-10-08T08:10:00",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Требуется аутентификация",
                                              "path": "/api/v1/couples/join"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Invite-код не найден",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Код не найден",
                                    value = """
                                            {
                                              "timestamp": "2026-10-08T08:10:00",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "Invite-код не найден",
                                              "path": "/api/v1/couples/join"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Присоединиться нельзя",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Партнёр уже есть",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-08T08:10:00",
                                                      "status": 409,
                                                      "error": "Conflict",
                                                      "message": "В этой паре уже есть партнер",
                                                      "path": "/api/v1/couples/join"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Собственное пространство",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-08T08:10:00",
                                                      "status": 409,
                                                      "error": "Conflict",
                                                      "message": "Вы не можете присоединиться к своей же паре",
                                                      "path": "/api/v1/couples/join"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Пользователь уже в паре",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-08T08:10:00",
                                                      "status": 409,
                                                      "error": "Conflict",
                                                      "message": "Пользователь уже находится в паре",
                                                      "path": "/api/v1/couples/join"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    })
    @PostMapping(
            value = "/join",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<CoupleResponseDto> join(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody JoinCoupleRequestDto request
            ) {
        return ResponseEntity.ok(coupleService.joinCouple(currentUser, request));
    }

    @Operation(
            summary = "Получить список участников пространства",
            description = "Возвращает участников пространства текущего пользователя"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Список участников",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CoupleMemberResponseDto.class)),
                            examples = @ExampleObject(
                                    name = "Участники",
                                    value = """
                                            [
                                              {
                                                "id": "6f9619ff-8b86-d011-b42d-00cf4fc964ff",
                                                "displayName": "Buratino"
                                              },
                                              {
                                                "id": "7a1b2c3d-4e5f-6a7b-8c9d-0e1f2a3b4c5d",
                                                "displayName": "Malvina"
                                              }
                                            ]
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
                                    name = "Нет аутентификации",
                                    value = """
                                            {
                                              "timestamp": "2026-10-08T08:10:00",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Требуется аутентификация",
                                              "path": "/api/v1/couples/me/members"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "У пользователя нет пространства",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Пространство не найдено",
                                    value = """
                                            {
                                              "timestamp": "2026-10-08T08:10:00",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "Пара пользователя не найдена",
                                              "path": "/api/v1/couples/me/members"
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
                                              "timestamp": "2026-10-08T08:10:00",
                                              "status": 409,
                                              "error": "Conflict",
                                              "message": "Пространство ещё не активно",
                                              "path": "/api/v1/couples/me/members"
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping(
            value = "/me/members",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<CoupleMemberResponseDto>> getMembers(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(coupleService.getMembers(currentUser));
    }
}
