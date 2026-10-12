package com.floweapp.flowe_api.auth.controller;

import com.floweapp.flowe_api.auth.dto.AuthResponseDto;
import com.floweapp.flowe_api.auth.dto.LoginRequestDto;
import com.floweapp.flowe_api.auth.dto.RefreshRequestDto;
import com.floweapp.flowe_api.auth.dto.RegisterRequestDto;
import com.floweapp.flowe_api.auth.service.AuthService;
import com.floweapp.flowe_api.common.ErrorResponse;

import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Authentication",
        description = "Регистрация, вход, выход и обновление токенов"
)
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Зарегистрировать пользователя",
            description = "Создаёт аккаунт и возвращает access и refresh токены"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Пользователь успешно зарегистрирован",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthResponseDto.class),
                            examples = @ExampleObject(
                                    name = "Успешная регистрация",
                                    value = """
                                            {
                                              "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                                              "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
                                              "expiresIn": 900
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
                                            name = "Невалидный email",
                                            summary = "Email не соответствует формату",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-08T01:34:00",
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "email: Email должен быть корректным",
                                                      "path": "/api/v1/auth/register"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Невалидный пароль",
                                            summary = "Пароль не соответствует ограничениям длины",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-08T01:34:00",
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "password: Пароль должен содержать от 8 до 72 символов",
                                                      "path": "/api/v1/auth/register"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Невалидное имя",
                                            summary = "Имя не соответствует ограничениям длины",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-08T01:34:00",
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "displayName: Имя должно содержать от 3 до 100 символов",
                                                      "path": "/api/v1/auth/register"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Пользователь с таким email уже существует",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Email уже занят",
                                    value = """
                                            {
                                              "timestamp": "2026-10-08T01:34:00",
                                              "status": 409,
                                              "error": "Conflict",
                                              "message": "Пользователь с таким email уже существует",
                                              "path": "/api/v1/auth/register"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "415",
                    description = "Неподдерживаемый Content-Type. Ожидается application/json",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Неподдерживаемый Content-Type",
                                    value = """
                                            {
                                              "timestamp": "2026-10-08T01:34:00",
                                              "status": 415,
                                              "error": "Unsupported Media Type",
                                              "message": "Content-Type 'text/plain' is not supported",
                                              "path": "/api/v1/auth/register"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping(
            value = "/register",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AuthResponseDto> register(
            @Valid @RequestBody RegisterRequestDto request
    ) {
        AuthResponseDto response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(
            summary = "Войти в аккаунт",
            description = "Проверяет email и пароль, затем возвращает access и refresh токены"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешная аутентификация",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthResponseDto.class),
                            examples = @ExampleObject(
                                    name = "Успешный вход",
                                    value = """
                                            {
                                              "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                                              "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
                                              "expiresIn": 900
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
                                            name = "Пустой email",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-08T01:34:00",
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "email: Email не может быть пустым; email: Email должен иметь формат адреса электронной почты",
                                                      "path": "/api/v1/auth/login"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Пустой пароль",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-08T01:34:00",
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "password: Пароль не может быть пустым",
                                                      "path": "/api/v1/auth/login"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Неверный email или пароль",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Неверные учётные данные",
                                    value = """
                                            {
                                              "timestamp": "2026-10-08T01:34:00",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Неверный email или пароль",
                                              "path": "/api/v1/auth/login"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "415",
                    description = "Неподдерживаемый Content-Type. Ожидается application/json",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Неподдерживаемый Content-Type",
                                    value = """
                                            {
                                              "timestamp": "2026-10-08T01:34:00",
                                              "status": 415,
                                              "error": "Unsupported Media Type",
                                              "message": "Content-Type 'text/plain' is not supported",
                                              "path": "/api/v1/auth/login"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping(
            value = "/login",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AuthResponseDto> login(
            @Valid @RequestBody LoginRequestDto request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(
            summary = "Обновить access token",
            description = "Проверяет refresh token и возвращает новый access token"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Access token успешно обновлён",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthResponseDto.class),
                            examples = @ExampleObject(
                                    name = "Успешное обновление токена",
                                    value = """
                                            {
                                              "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                                              "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
                                              "expiresIn": 900
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Refresh token не передан",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Пустой refresh token",
                                    value = """
                                            {
                                              "timestamp": "2026-10-08T01:34:00",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "refreshToken: Refresh token не может быть пустым",
                                              "path": "/api/v1/auth/refresh"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Refresh token недействителен или просрочен",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Токен не найден",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-08T01:34:00",
                                                      "status": 401,
                                                      "error": "Unauthorized",
                                                      "message": "Refresh токен не найден",
                                                      "path": "/api/v1/auth/refresh"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Токен просрочен",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-08T01:34:00",
                                                      "status": 401,
                                                      "error": "Unauthorized",
                                                      "message": "Refresh токен просрочен",
                                                      "path": "/api/v1/auth/refresh"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "Неверный токен",
                                            value = """
                                                    {
                                                      "timestamp": "2026-10-08T01:34:00",
                                                      "status": 401,
                                                      "error": "Unauthorized",
                                                      "message": "Неверный refresh токен",
                                                      "path": "/api/v1/auth/refresh"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "415",
                    description = "Неподдерживаемый Content-Type. Ожидается application/json",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Неподдерживаемый Content-Type",
                                    value = """
                                            {
                                              "timestamp": "2026-10-08T01:34:00",
                                              "status": 415,
                                              "error": "Unsupported Media Type",
                                              "message": "Content-Type 'text/plain' is not supported",
                                              "path": "/api/v1/auth/refresh"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping(
            value = "/refresh",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AuthResponseDto> refresh(
            @Valid @RequestBody RefreshRequestDto request
    ) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @Operation(
            summary = "Выйти из аккаунта",
            description = "Удаляет refresh token и завершает текущую сессию"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Выход выполнен успешно"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Refresh token не передан",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Пустой refresh token",
                                    value = """
                                            {
                                              "timestamp": "2026-10-08T01:34:00",
                                              "status": 400,
                                              "error": "Bad Request",
                                              "message": "refreshToken: Refresh token не может быть пустым",
                                              "path": "/api/v1/auth/logout"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "415",
                    description = "Неподдерживаемый Content-Type. Ожидается application/json",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Неподдерживаемый Content-Type",
                                    value = """
                                            {
                                              "timestamp": "2026-10-08T01:34:00",
                                              "status": 415,
                                              "error": "Unsupported Media Type",
                                              "message": "Content-Type 'text/plain' is not supported",
                                              "path": "/api/v1/auth/logout"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping(
            value = "/logout",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Void> logout(
            @Valid @RequestBody RefreshRequestDto request
    ) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }
}