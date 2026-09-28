package com.floweapp.flowe_api.couple.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "couples")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Couple {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "couple_status")
    private CoupleStatus status;

    @Column(name = "user1_id", unique = true)
    private UUID user1Id;

    @Column(name = "user2_id", unique = true)
    private UUID user2Id;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (status == null) status = CoupleStatus.pending;
    }

    public boolean isPending() {
        return status == CoupleStatus.pending;
    }

    public boolean isActive() {
        return status == CoupleStatus.active;
    }
}
