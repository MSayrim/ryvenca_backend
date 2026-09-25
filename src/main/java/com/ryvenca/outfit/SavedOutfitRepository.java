package com.ryvenca.outfit;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedOutfitRepository extends JpaRepository<SavedOutfit, Long> {

    List<SavedOutfit> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    Optional<SavedOutfit> findByOwnerIdAndOutfitKey(Long ownerId, String outfitKey);

    Optional<SavedOutfit> findByIdAndOwnerId(Long id, Long ownerId);

    long countByOwnerId(Long ownerId);
}
