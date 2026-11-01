package com.floweapp.flowe_api;

import com.floweapp.flowe_api.task.dto.CreateTaskRequestDto;
import com.floweapp.flowe_api.task.entity.TaskStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Tasks API")
public class TasksTest extends TestSupport {

    private static final String COUPLE_NOT_FOUND = "Пользователь не состоит в паре";
    private static final String COUPLE_NOT_ACTIVE = "Пара пользователя неактивна";
    private static final String TASK_NOT_FOUND = "Задача не найдена";

    @Test
    @DisplayName("TK-01: Участник пары создаёт задачу, партнёр видит её с зеркальными флагами")
    void userCanCreateTaskAndPartnerSeesIt() throws Exception {
        ActiveCouple couple = createActiveCouple();
        CreateTaskRequestDto dto = uniqueCreateTaskRequestDto();

        JsonNode created = createTaskSuccessfully(couple.ownerToken(), dto);

        getTask(couple.partnerToken(), created.path("id").asString())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(dto.title()))
                .andExpect(jsonPath("$.status").value("todo"))
                .andExpect(jsonPath("$.assignedToMe").value(false))
                .andExpect(jsonPath("$.assignedToPartner").value(true));
    }

    @DisplayName("Запрос без access-токена отклоняется")
    @ParameterizedTest(name = "{0}: {1} {2} без токена -> 401")
    @CsvSource({
            "TK-02, POST, /api/v1/tasks",
            "TK-03, GET,  /api/v1/tasks",
            "TK-04, GET,  /api/v1/tasks/00000000-0000-0000-0000-000000000001"
    })
    void requestWithoutTokenIsRejected(String caseId, String method, String url) throws Exception {
        MvcResult result = mockMvc.perform(request(HttpMethod.valueOf(method), url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn();

        assertRejected(result, 401);
    }

    @Test
    @DisplayName("TK-05: Пользователь без пары не может создать задачу (404)")
    void userWithoutCoupleCannotCreateTask() throws Exception {
        String token = registerAndGetAccessToken();

        assertRejected(createTask(token, uniqueCreateTaskRequestDto())
                .andExpect(jsonPath("$.message").value(COUPLE_NOT_FOUND))
                .andReturn(), 404);
    }

    @Test
    @DisplayName("TK-06: Пользователь с неактивной парой не может создать задачу (409)")
    void userWithPendingCoupleCannotCreateTask() throws Exception {
        String token = registerAndGetAccessToken();
        createCoupleSuccessfully(token, uniqueCoupleNameRequestDto());

        assertRejected(createTask(token, uniqueCreateTaskRequestDto())
                .andExpect(jsonPath("$.message").value(COUPLE_NOT_ACTIVE))
                .andReturn(), 409);
    }

    @DisplayName("Валидация названия задачи")
    @ParameterizedTest(name = "{0}: название длиной {1} -> HTTP {2}")
    @CsvSource({
            "TK-07, 0,   400",
            "TK-08, 100, 201",
            "TK-09, 101, 400"
    })
    void titleLengthBoundary(String caseId, int length, int expectedStatus) throws Exception {
        String token = createActiveCouple().ownerToken();
        CreateTaskRequestDto dto = new CreateTaskRequestDto("a".repeat(length), null, true, false, null);

        MvcResult result = createTask(token, dto).andReturn();

        assertEquals(expectedStatus, result.getResponse().getStatus(), caseId + ": неожиданный статус");
    }

    @Test
    @DisplayName("TK-10: Дедлайн в прошлом даёт 400")
    void pastDueDateIsRejected() throws Exception {
        String token = createActiveCouple().ownerToken();
        CreateTaskRequestDto dto = new CreateTaskRequestDto(
                "task", null, true, false, OffsetDateTime.now().minusDays(1));

        assertRejected(createTask(token, dto).andReturn(), 400);
    }

    @Test
    @DisplayName("TK-11: Пользователь из другой пары не видит чужую задачу (404)")
    void taskOfAnotherCoupleIsNotAccessible() throws Exception {
        String ownerToken = createActiveCouple().ownerToken();
        String strangerToken = createActiveCouple().ownerToken();
        String taskId = createTaskSuccessfully(ownerToken, uniqueCreateTaskRequestDto()).path("id").asString();

        assertRejected(getTask(strangerToken, taskId)
                .andExpect(jsonPath("$.message").value(TASK_NOT_FOUND))
                .andReturn(), 404);
    }

    @Test
    @DisplayName("TK-12: PATCH меняет название и описание")
    void userCanUpdateTask() throws Exception {
        String token = createActiveCouple().ownerToken();
        String taskId = createTaskSuccessfully(token, uniqueCreateTaskRequestDto()).path("id").asString();

        updateTask(token, taskId, "{\"title\":\"Новое название\",\"description\":\"Описание\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Новое название"))
                .andExpect(jsonPath("$.description").value("Описание"));

        getTask(token, taskId).andExpect(jsonPath("$.title").value("Новое название"));
    }

    @Test
    @DisplayName("TK-13: Статус done проставляет completedAt, возврат в todo сбрасывает его")
    void doneStatusSetsAndClearsCompletedAt() throws Exception {
        String token = createActiveCouple().ownerToken();
        String taskId = createTaskSuccessfully(token, uniqueCreateTaskRequestDto()).path("id").asString();

        updateTaskStatus(token, taskId, TaskStatus.done)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("done"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());

        updateTaskStatus(token, taskId, TaskStatus.todo)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("todo"))
                .andExpect(jsonPath("$.completedAt").isEmpty());
    }

    @Test
    @DisplayName("TK-14: Недопустимый переход closed -> in_progress даёт 409 и не меняет статус")
    void invalidStatusTransitionIsRejected() throws Exception {
        String token = createActiveCouple().ownerToken();
        String taskId = createTaskSuccessfully(token, uniqueCreateTaskRequestDto()).path("id").asString();
        updateTaskStatus(token, taskId, TaskStatus.closed).andExpect(status().isOk());

        assertRejected(updateTaskStatus(token, taskId, TaskStatus.in_progress)
                .andExpect(jsonPath("$.message")
                        .value("Невозможно сменить статус задачи с 'closed' на 'in_progress'"))
                .andReturn(), 409);

        getTask(token, taskId).andExpect(jsonPath("$.status").value("closed"));
    }

    @Test
    @DisplayName("TK-15: Удалённая задача возвращает 204, затем 404")
    void userCanDeleteTask() throws Exception {
        String token = createActiveCouple().ownerToken();
        String taskId = createTaskSuccessfully(token, uniqueCreateTaskRequestDto()).path("id").asString();

        deleteTask(token, taskId).andExpect(status().isNoContent());

        assertRejected(getTask(token, taskId).andReturn(), 404);
    }

    @Test
    @DisplayName("TK-16: Курсорная пагинация возвращает все задачи без повторов")
    void cursorPaginationWorks() throws Exception {
        String token = createActiveCouple().ownerToken();
        for (int i = 0; i < 3; i++) {
            createTaskSuccessfully(token, uniqueCreateTaskRequestDto());
        }

        MvcResult first = listTasks(token, 2, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.hasMore").value(true))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty())
                .andReturn();

        String cursor = responseJson(first).path("nextCursor").asString();

        listTasks(token, 2, cursor)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.hasMore").value(false));
    }
}