package com.ryvenca.image;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<ImageAsset, UUID> {

    Optional<ImageAsset> findByIdAndOwnerId(UUID id, Long ownerId);

    List<ImageAsset> findByOwnerId(Long ownerId);

    List<ImageAsset> findByAttachedFalseAndCreatedAtBefore(Instant cutoff);
}
