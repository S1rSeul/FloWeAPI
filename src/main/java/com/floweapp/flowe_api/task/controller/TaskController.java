package com.floweapp.flowe_api.task.controller;

import com.floweapp.flowe_api.common.dto.CursorPageResponseDto;
import com.floweapp.flowe_api.task.dto.CreateTaskRequestDto;
import com.floweapp.flowe_api.task.dto.TaskResponseDto;
import com.floweapp.flowe_api.task.service.TaskService;
import com.floweapp.flowe_api.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponseDto> create(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateTaskRequestDto request
            ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.createTask(currentUser, request));
    }

    @GetMapping
    public ResponseEntity<CursorPageResponseDto<TaskResponseDto>> list(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor
    ) {
        return ResponseEntity.ok(taskService.listTasks(currentUser, sort, order, limit, cursor));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponseDto> getById(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(taskService.getTask(currentUser, id));
    }
}
