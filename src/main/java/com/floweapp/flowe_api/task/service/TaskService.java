package com.floweapp.flowe_api.task.service;

import com.floweapp.flowe_api.common.dto.CursorPageResponseDto;
import com.floweapp.flowe_api.couple.entity.Couple;
import com.floweapp.flowe_api.couple.exception.CoupleNotFoundException;
import com.floweapp.flowe_api.couple.repository.CoupleRepository;
import com.floweapp.flowe_api.task.dto.CreateTaskRequestDto;
import com.floweapp.flowe_api.task.dto.TaskResponseDto;
import com.floweapp.flowe_api.task.entity.Task;
import com.floweapp.flowe_api.task.entity.TaskStatus;
import com.floweapp.flowe_api.task.exception.CoupleNotActiveException;
import com.floweapp.flowe_api.task.exception.InvalidQueryParameterException;
import com.floweapp.flowe_api.task.exception.TaskNotFoundException;
import com.floweapp.flowe_api.task.repository.TaskRepository;
import com.floweapp.flowe_api.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final TaskRepository taskRepository;
    private final CoupleRepository coupleRepository;

    @Transactional
    public TaskResponseDto createTask(User currentUser, CreateTaskRequestDto request) {
        Couple couple = getCoupleOrThrow(currentUser.getId());
        boolean isCurrentUserUser1 = couple.getUser1Id().equals(currentUser.getId());

        boolean isUser1Assignee = isCurrentUserUser1
                ? request.assignedToMe()
                : request.assignedToPartner();
        boolean isUser2Assignee = isCurrentUserUser1
                ? request.assignedToPartner()
                : request.assignedToMe();

        Task task = Task.builder()
                .coupleId(couple.getId())
                .title(request.title())
                .description(request.description())
                .createdById(currentUser.getId())
                .isUser1Assignee(isUser1Assignee)
                .isUser2Assignee(isUser2Assignee)
                .dueDate(request.dueDate() == null ? null : request.dueDate())
                .status(TaskStatus.todo)
                .build();

        Task saved = taskRepository.save(task);
        return toResponse(saved, couple, currentUser.getId());
    }

    @Transactional(readOnly = true)
    public CursorPageResponseDto<TaskResponseDto> listTasks(
            User currentUser,
            String sort,
            String order,
            Integer limit,
            String cursor
    ) {
        Couple couple = getCoupleOrThrow(currentUser.getId());
        TaskSort taskSort = TaskSort.parse(sort, order);

        int pageSize = limit == null ? DEFAULT_LIMIT : limit;
        if (pageSize < 1 || pageSize > MAX_LIMIT) {
            throw new InvalidQueryParameterException("limit должен быть от 1 до " + MAX_LIMIT);
        }

        boolean hasCursor = cursor != null && !cursor.isBlank();
        OffsetDateTime cursorAt = null;
        UUID cursorId = null;
        if (hasCursor) {
            TaskCursor.Decoded decoded = TaskCursor.decode(cursor);
            cursorAt = decoded.value();
            cursorId = decoded.id();
        }

        PageRequest pageRequest = PageRequest.of(0, pageSize + 1);
        UUID coupleId = couple.getId();

        List<Task> tasks = switch (taskSort) {
            case CREATED_AT_DESC -> hasCursor
                    ? taskRepository.findNextPageByCreatedAtDesc(coupleId, cursorAt, cursorId, pageRequest)
                    : taskRepository.findFirstPageByCreatedAtDesc(coupleId, pageRequest);
            case CREATED_AT_ASC -> hasCursor
                    ? taskRepository.findNextPageByCreatedAtAsc(coupleId, cursorAt, cursorId, pageRequest)
                    : taskRepository.findFirstPageByCreatedAtAsc(coupleId, pageRequest);
            case DUE_DATE_ASC -> hasCursor
                    ? taskRepository.findNextPageByDueDateAsc(coupleId, cursorAt, cursorId, pageRequest)
                    : taskRepository.findFirstPageByDueDateAsc(coupleId, pageRequest);
            case DUE_DATE_DESC -> hasCursor
                    ? taskRepository.findNextPageByDueDateDesc(coupleId, cursorAt, cursorId, pageRequest)
                    : taskRepository.findFirstPageByDueDateDesc(coupleId, pageRequest);
        };

        boolean hasMore = tasks.size() > pageSize;
        if (hasMore) {
            tasks = tasks.subList(0, pageSize);
        }

        String nextCursor = null;
        if (hasMore && !tasks.isEmpty()) {
            Task last = tasks.getLast();
            OffsetDateTime sortValue = taskSort.isDueDate() ? last.getDueDate() : last.getCreatedAt();
            nextCursor = TaskCursor.encode(sortValue, last.getId());
        }

        UUID currentUserId = currentUser.getId();
        List<TaskResponseDto> items = tasks.stream()
                .map(t -> toResponse(t, couple, currentUserId))
                .toList();

        return new CursorPageResponseDto<>(items, nextCursor, hasMore);
    }

    @Transactional(readOnly = true)
    public TaskResponseDto getTask(User currentUser, UUID taskId) {
        Couple couple = getCoupleOrThrow(currentUser.getId());

        Task task = taskRepository.findByIdAndCoupleId(taskId, couple.getId())
                .orElseThrow(TaskNotFoundException::new);

        return toResponse(task, couple, currentUser.getId());
    }

    private Couple getCoupleOrThrow(UUID userId) {
        Couple couple = coupleRepository.findByUserId(userId)
                .orElseThrow(CoupleNotFoundException::new);

        if (!couple.isActive()) {
            throw new CoupleNotActiveException();
        }

        return couple;
    }

    private TaskResponseDto toResponse(Task task, Couple couple, UUID currentUserId) {
        boolean isCurrentUserUser1 = couple.getUser1Id().equals(currentUserId);

        boolean assignedToMe = isCurrentUserUser1
                ? task.isUser1Assignee()
                : task.isUser2Assignee();
        boolean assignedToPartner = isCurrentUserUser1
                ? task.isUser2Assignee()
                : task.isUser1Assignee();

        boolean isOverdue = task.getDueDate() != null
                && task.getDueDate().isBefore(OffsetDateTime.now())
                && task.getStatus() != TaskStatus.done
                && task.getStatus() != TaskStatus.closed;

        return new TaskResponseDto(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getCreatedById(),
                assignedToMe,
                assignedToPartner,
                task.getDueDate(),
                task.getStatus(),
                isOverdue,
                task.getCompletedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
