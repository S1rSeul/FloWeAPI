package com.floweapp.flowe_api;

import com.floweapp.flowe_api.task.dto.CreateTaskRequestDto;
import com.floweapp.flowe_api.task.entity.Task;
import com.floweapp.flowe_api.task.entity.TaskStatus;
import com.floweapp.flowe_api.task.repository.TaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Task deadlines API")
public class TaskDeadlinesTest extends TestSupport {

    @Autowired
    private TaskRepository taskRepository;

    @Test
    @DisplayName("TD-01: Создание задачи без дедлайна")
    void taskCanBeCreatedWithoutDueDate() throws Exception {
        String token = createActiveCouple().ownerToken();
        String taskId = createTaskWithDueDate(token, null);

        getTask(token, taskId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueDate").isEmpty())
                .andExpect(jsonPath("$.isOverdue").value(false));
    }

    @Test
    @DisplayName("TD-02: Дедлайн в часовом поясе +04:00 сохраняет момент времени")
    void dueDateWithPlusFourOffsetIsSaved() throws Exception {
        String token = createActiveCouple().ownerToken();
        OffsetDateTime dueDate = OffsetDateTime.now(ZoneOffset.of("+04:00")).plusDays(1);

        String taskId = createTaskWithDueDate(token, dueDate);

        JsonNode task = responseJson(getTask(token, taskId).andExpect(status().isOk()).andReturn());

        assertSameInstant(dueDate, task.path("dueDate").asString());
        assertFalse(task.path("isOverdue").asBoolean());
    }

    @Test
    @DisplayName("TD-03: Дедлайн в UTC (Z) сохраняет момент времени")
    void dueDateWithUtcOffsetIsSaved() throws Exception {
        String token = createActiveCouple().ownerToken();
        OffsetDateTime dueDate = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);

        String taskId = createTaskWithDueDate(token, dueDate);

        JsonNode task = responseJson(getTask(token, taskId).andExpect(status().isOk()).andReturn());

        assertSameInstant(dueDate, task.path("dueDate").asString());
        assertFalse(task.path("isOverdue").asBoolean());
    }

    @Test
    @DisplayName("TD-04: Дедлайн в часовом поясе -05:00 сохраняет момент времени")
    void dueDateWithMinusFiveOffsetIsSaved() throws Exception {
        String token = createActiveCouple().ownerToken();
        OffsetDateTime dueDate = OffsetDateTime.now(ZoneOffset.of("-05:00")).plusDays(1);

        String taskId = createTaskWithDueDate(token, dueDate);

        JsonNode task = responseJson(getTask(token, taskId).andExpect(status().isOk()).andReturn());

        assertSameInstant(dueDate, task.path("dueDate").asString());
        assertFalse(task.path("isOverdue").asBoolean());
    }

    @Test
    @DisplayName("TD-05: Дедлайн в прошлом при создании даёт 400")
    void pastDueDateIsRejectedOnCreate() throws Exception {
        String token = createActiveCouple().ownerToken();
        OffsetDateTime pastDueDate = OffsetDateTime.now().minusDays(1);

        assertRejected(createTask(token, new CreateTaskRequestDto(
                "task-with-past-deadline", null, false, false, pastDueDate)).andReturn(), 400);
    }

    @Test
    @DisplayName("TD-06: PATCH с dueDate=null сбрасывает дедлайн")
    void dueDateCanBeCleared() throws Exception {
        String token = createActiveCouple().ownerToken();
        String taskId = createTaskWithDueDate(token, OffsetDateTime.now().plusDays(1));

        updateTask(token, taskId, "{\"dueDate\":null}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueDate").isEmpty())
                .andExpect(jsonPath("$.isOverdue").value(false));

        getTask(token, taskId)
                .andExpect(jsonPath("$.dueDate").isEmpty())
                .andExpect(jsonPath("$.isOverdue").value(false));
    }

    @Test
    @DisplayName("TD-07: Просроченная незавершённая задача помечается isOverdue=true")
    void overdueTodoTaskIsMarkedAsOverdue() throws Exception {
        String token = createActiveCouple().ownerToken();
        String taskId = createTaskWithDueDate(token, OffsetDateTime.now().plusDays(1));

        Task task = taskRepository.findById(UUID.fromString(taskId)).orElseThrow();
        task.setDueDate(OffsetDateTime.now().minusDays(1));
        taskRepository.save(task);

        getTask(token, taskId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueDate").isNotEmpty())
                .andExpect(jsonPath("$.status").value("todo"))
                .andExpect(jsonPath("$.isOverdue").value(true));

        updateTaskStatus(token, taskId, TaskStatus.done)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isOverdue").value(false));
    }

    private void assertSameInstant(OffsetDateTime expected, String actualDueDate) {
        OffsetDateTime actual = OffsetDateTime.parse(actualDueDate);

        assertTrue(
                Duration.between(expected, actual).abs().compareTo(Duration.ofNanos(1_000)) <= 0,
                "Момент времени дедлайна должен сохраниться независимо от офсета"
        );
    }
}