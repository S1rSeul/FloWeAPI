package com.floweapp.flowe_api.task.controller;

import com.floweapp.flowe_api.common.dto.CursorPageResponseDto;
import com.floweapp.flowe_api.task.dto.*;
import com.floweapp.flowe_api.task.service.TaskService;
import com.floweapp.flowe_api.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public TaskResponseDto create(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateTaskRequestDto request
    ) {
        return taskService.createTask(currentUser, request);
    }

    @GetMapping
    public CursorPageResponseDto<TaskResponseDto> list(
            @AuthenticationPrincipal User currentUser,
            @Valid TaskListQueryDto query
    ) {
        return taskService.listTasks(currentUser, query);
    }

    @GetMapping("/{id}")
    public TaskResponseDto getById(@AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
        return taskService.getTask(currentUser, id);
    }

    @PatchMapping("/{id}")
    public TaskResponseDto update(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTaskRequestDto request
    ) {
        return taskService.updateTask(currentUser, id, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
        taskService.deleteTask(currentUser, id);
    }

    @PatchMapping("/{id}/status")
    public TaskResponseDto updateStatus(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTaskStatusRequestDto request
    ) {
        return taskService.updateStatus(currentUser, id, request);
    }
}