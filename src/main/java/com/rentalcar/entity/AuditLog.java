package com.rentalcar.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(
    name = "audit_logs",
    indexes = {
        @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_audit_actor", columnList = "actor"),
        @Index(name = "idx_audit_created_at", columnList = "created_at")
    }
)
@Immutable
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;    // "Booking", "Car", "User"

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "action", nullable = false, length = 100)
    private String action;        // "CREATE", "STATUS_CHANGE", "UPDATE", "DELETE"

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;      // JSON snapshot before

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;      // JSON snapshot after

    @Column(name = "actor", length = 100)
    private String actor;         // username or "SYSTEM"

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;       // human-readable description
}
