package com.ryvenca.deletion;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeletionRequestRepository extends JpaRepository<DeletionRequest, Long> {

    Optional<DeletionRequest> findFirstByEmailAndStatus(String email, DeletionRequestStatus status);

    List<DeletionRequest> findByStatusOrderByCreatedAtDesc(DeletionRequestStatus status);

    List<DeletionRequest> findAllByOrderByCreatedAtDesc();

    long countByStatus(DeletionRequestStatus status);

    boolean existsByReference(String reference);
}
