package com.floweapp.flowe_api.calendar.repository;

import com.floweapp.flowe_api.calendar.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {
    Optional<Event> findByIdAndCoupleId(UUID id, UUID coupleId);

    @Query("""
        SELECT e FROM Event e
        WHERE e.coupleId = :coupleId
          AND e.startsAt < :to
          AND COALESCE(e.endsAt, e.startsAt) >= :from
        ORDER BY e.startsAt ASC, e.id ASC
    """)
    List<Event> findInRange(
            @Param("coupleId") UUID coupleId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );
}
