# FloWeAPI

Backend приложения для пар

# Содержание

- [Быстрый старт](#быстрый-старт)
- [Миграции Flyway](#миграции-flyway)
- [Тесты](#тесты)
- [API и Swagger](#api-и-swagger)

---

# Быстрый старт

## 1. Поднять PostgreSQL в Docker
`docker compose up -d db`

## 2. Создать конфигурацию запуска
Указать `com.floweapp.flowe_api.FloweApiApplication` в Main Class  
Добавить `spring.profiles.active=dev` в переменные окружения там же

## 3. Запустить приложение
Приложение доступно на http://localhost:8080  
Подключен актуатор с health и info  
Проверка жизнеспособности на http://localhost:8080/actuator/health

---

# Миграции Flyway

Схема БД управляется только через Flyway. **Схему руками не менять**  
Flyway применяет миграцию при старте приложения автоматически

## Где лежат миграции

src/main/resources/db/migration/  
├── V1__init.sql  
├── V2__create_couples.sql  
└── V3__create_tasks.sql  

**Именование**: V{номер}__{описание}.sql — двойное подчёркивание обязательно.

---

# Тесты

## Запустить все тесты
`./mvnw verify`

Тесты используют Testcontainers — PostgreSQL поднимается в Docker автоматически. Локальная БД для тестов не нужна

## Покрытие
Отчёт JaCoCo появится в:
`target/site/jacoco/index.html`

---

# API и Swagger

## Swagger
http://localhost:8080/swagger-ui.html

## OpenAPI JSON
http://localhost:8080/v3/api-docs

## Формат ошибок
Все ошибки возвращаются в едином формате:  

| Property  | Value                                          |
| --------- | ---------------------------------------------- |
| timestamp | 2026-09-22T19:05:38                            |
| status    | 500                                            |
| error     | Internal server error                          |
| message   | No static resource aboba for request '/aboba'. |
| path      | /aboba                                         |
