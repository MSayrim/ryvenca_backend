package com.ryvenca.deletion;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeletionLogRepository extends JpaRepository<DeletionLogEntry, Long> {

    List<DeletionLogEntry> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByCreatedAtAfter(Instant since);
}
