package com.floweapp.flowe_api;

import com.floweapp.flowe_api.task.dto.CreateTaskRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Task assignees API")
public class TaskAssigneesTest extends TestSupport {

    @Test
    @DisplayName("TA-01: Пользователь назначает задачу только себе")
    void userCanAssignTaskToSelf() throws Exception {
        String token = createActiveCouple().ownerToken();
        String taskId = createTaskSuccessfully(token, uniqueCreateTaskRequestDto()).path("id").asString();

        updateTaskAssignees(token, taskId, true, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedToMe").value(true))
                .andExpect(jsonPath("$.assignedToPartner").value(false));

        getTask(token, taskId)
                .andExpect(jsonPath("$.assignedToMe").value(true))
                .andExpect(jsonPath("$.assignedToPartner").value(false));
    }

    @Test
    @DisplayName("TA-02: Пользователь назначает задачу только партнёру")
    void userCanAssignTaskToPartner() throws Exception {
        String token = createActiveCouple().ownerToken();
        String taskId = createTaskSuccessfully(token, uniqueCreateTaskRequestDto()).path("id").asString();

        updateTaskAssignees(token, taskId, false, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedToMe").value(false))
                .andExpect(jsonPath("$.assignedToPartner").value(true));

        getTask(token, taskId)
                .andExpect(jsonPath("$.assignedToMe").value(false))
                .andExpect(jsonPath("$.assignedToPartner").value(true));
    }

    @Test
    @DisplayName("TA-03: Пользователь назначает задачу себе и партнёру")
    void userCanAssignTaskToBoth() throws Exception {
        String token = createActiveCouple().ownerToken();
        String taskId = createTaskSuccessfully(token, uniqueCreateTaskRequestDto()).path("id").asString();

        updateTaskAssignees(token, taskId, true, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedToMe").value(true))
                .andExpect(jsonPath("$.assignedToPartner").value(true));

        getTask(token, taskId)
                .andExpect(jsonPath("$.assignedToMe").value(true))
                .andExpect(jsonPath("$.assignedToPartner").value(true));
    }

    @Test
    @DisplayName("TA-04: Пользователь снимает назначение с себя и партнёра")
    void userCanUnassignTaskFromBoth() throws Exception {
        String token = createActiveCouple().ownerToken();
        CreateTaskRequestDto dto = new CreateTaskRequestDto(
                "task", null, true, true, null);
        String taskId = createTaskSuccessfully(token, dto).path("id").asString();

        updateTaskAssignees(token, taskId, false, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedToMe").value(false))
                .andExpect(jsonPath("$.assignedToPartner").value(false));

        getTask(token, taskId)
                .andExpect(jsonPath("$.assignedToMe").value(false))
                .andExpect(jsonPath("$.assignedToPartner").value(false));
    }

    @Test
    @DisplayName("TA-05: Пользователь снимает назначение только с себя")
    void userCanUnassignTaskFromSelf() throws Exception {
        String token = createActiveCouple().ownerToken();
        CreateTaskRequestDto dto = new CreateTaskRequestDto(
                "task", null, true, false, null);
        String taskId = createTaskSuccessfully(token, dto).path("id").asString();

        updateTaskAssignees(token, taskId, false, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedToMe").value(false))
                .andExpect(jsonPath("$.assignedToPartner").value(false));

        getTask(token, taskId)
                .andExpect(jsonPath("$.assignedToMe").value(false))
                .andExpect(jsonPath("$.assignedToPartner").value(false));
    }

    @Test
    @DisplayName("TA-06: Пользователь снимает назначение только с партнёра")
    void userCanUnassignTaskFromPartner() throws Exception {
        String token = createActiveCouple().ownerToken();
        CreateTaskRequestDto dto = new CreateTaskRequestDto(
                "task", null, false, true, null);
        String taskId = createTaskSuccessfully(token, dto).path("id").asString();

        updateTaskAssignees(token, taskId, null, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedToMe").value(false))
                .andExpect(jsonPath("$.assignedToPartner").value(false));

        getTask(token, taskId)
                .andExpect(jsonPath("$.assignedToMe").value(false))
                .andExpect(jsonPath("$.assignedToPartner").value(false));
    }

    @Test
    @DisplayName("TA-07: Партнёр назначает себя и снимает владельца")
    void partnerCanReassignTaskToSelf() throws Exception {
        ActiveCouple couple = createActiveCouple();
        CreateTaskRequestDto dto = new CreateTaskRequestDto(
                "task", null, true, false, null);
        String taskId = createTaskSuccessfully(couple.ownerToken(), dto).path("id").asString();

        updateTaskAssignees(couple.partnerToken(), taskId, true, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedToMe").value(true))
                .andExpect(jsonPath("$.assignedToPartner").value(false));

        getTask(couple.partnerToken(), taskId)
                .andExpect(jsonPath("$.assignedToMe").value(true))
                .andExpect(jsonPath("$.assignedToPartner").value(false));

        getTask(couple.ownerToken(), taskId)
                .andExpect(jsonPath("$.assignedToMe").value(false))
                .andExpect(jsonPath("$.assignedToPartner").value(true));
    }

    @Test
    @DisplayName("TA-08: Частичное обновление не сбрасывает другое поле назначения")
    void partialAssigneeUpdateKeepsOtherField() throws Exception {
        String token = createActiveCouple().ownerToken();
        CreateTaskRequestDto dto = new CreateTaskRequestDto(
                "task", null, true, true, null);
        String taskId = createTaskSuccessfully(token, dto).path("id").asString();

        updateTaskAssignees(token, taskId, false, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedToMe").value(false))
                .andExpect(jsonPath("$.assignedToPartner").value(true));

        updateTaskAssignees(token, taskId, null, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedToMe").value(false))
                .andExpect(jsonPath("$.assignedToPartner").value(false));
    }
}