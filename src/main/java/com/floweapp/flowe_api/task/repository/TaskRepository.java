package com.floweapp.flowe_api.task.repository;

import com.floweapp.flowe_api.task.entity.Task;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    Optional<Task> findByIdAndCoupleId(UUID id, UUID coupleId);

    // ===== CreatedAt DESC =====

    @Query("""
        SELECT t FROM Task t
        WHERE t.coupleId = :coupleId
        ORDER BY t.createdAt DESC, t.id DESC
    """)
    List<Task> findFirstPageByCreatedAtDesc(
            @Param("coupleId") UUID coupleId,
            Pageable pageable
    );

    @Query("""
        SELECT t FROM Task t
        WHERE t.coupleId = :coupleId
          AND (t.createdAt < :cursorAt
               OR (t.createdAt = :cursorAt AND t.id < :cursorId))
        ORDER BY t.createdAt DESC, t.id DESC
    """)
    List<Task> findNextPageByCreatedAtDesc(
            @Param("coupleId") UUID coupleId,
            @Param("cursorAt") OffsetDateTime cursorAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    // ===== CreatedAt ASC =====

    @Query("""
        SELECT t FROM Task t
        WHERE t.coupleId = :coupleId
        ORDER BY t.createdAt ASC, t.id ASC
    """)
    List<Task> findFirstPageByCreatedAtAsc(
            @Param("coupleId") UUID coupleId,
            Pageable pageable
    );

    @Query("""
        SELECT t FROM Task t
        WHERE t.coupleId = :coupleId
          AND (t.createdAt > :cursorAt
               OR (t.createdAt = :cursorAt AND t.id > :cursorId))
        ORDER BY t.createdAt ASC, t.id ASC
    """)
    List<Task> findNextPageByCreatedAtAsc(
            @Param("coupleId") UUID coupleId,
            @Param("cursorAt") OffsetDateTime cursorAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    // ===== DueDate ASC =====

    @Query("""
        SELECT t FROM Task t
        WHERE t.coupleId = :coupleId
        ORDER BY t.dueDate ASC NULLS LAST, t.id ASC
    """)
    List<Task> findFirstPageByDueDateAsc(
            @Param("coupleId") UUID coupleId,
            Pageable pageable
    );

    @Query("""
        SELECT t FROM Task t
        WHERE t.coupleId = :coupleId
          AND (
               (t.dueDate IS NULL AND t.id > :cursorId)
            OR (t.dueDate IS NOT NULL AND :cursorAt IS NOT NULL
                AND (t.dueDate > :cursorAt
                     OR (t.dueDate = :cursorAt AND t.id > :cursorId)))
            OR (t.dueDate IS NOT NULL AND :cursorAt IS NULL)
          )
        ORDER BY t.dueDate ASC NULLS LAST, t.id ASC
    """)
    List<Task> findNextPageByDueDateAsc(
            @Param("coupleId") UUID coupleId,
            @Param("cursorAt") OffsetDateTime cursorAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    // ===== DueDate DESC =====

    @Query("""
        SELECT t FROM Task t
        WHERE t.coupleId = :coupleId
        ORDER BY t.dueDate DESC NULLS LAST, t.id ASC
    """)
    List<Task> findFirstPageByDueDateDesc(
            @Param("coupleId") UUID coupleId,
            Pageable pageable
    );

    @Query("""
        SELECT t FROM Task t
        WHERE t.coupleId = :coupleId
          AND (
               (t.dueDate IS NULL AND t.id > :cursorId)
            OR (t.dueDate IS NOT NULL AND :cursorAt IS NOT NULL
                AND (t.dueDate < :cursorAt
                     OR (t.dueDate = :cursorAt AND t.id > :cursorId)))
            OR (t.dueDate IS NOT NULL AND :cursorAt IS NULL)
          )
        ORDER BY t.dueDate DESC NULLS LAST, t.id ASC
    """)
    List<Task> findNextPageByDueDateDesc(
            @Param("coupleId") UUID coupleId,
            @Param("cursorAt") OffsetDateTime cursorAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );
}
