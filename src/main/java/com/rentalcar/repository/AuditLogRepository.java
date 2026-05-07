package com.rentalcar.repository;

import com.rentalcar.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByEntityTypeAndEntityId(
        String entityType, UUID entityId, Pageable pageable);

    Page<AuditLog> findByActor(String actor, Pageable pageable);

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
        String entityType, UUID entityId);

    List<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
        Instant from, Instant to);
}
