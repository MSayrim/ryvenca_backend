package com.ryvenca.garment;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GarmentRepository extends JpaRepository<Garment, Long> {

    List<Garment> findByOwnerIdOrderByCreatedAtDescIdDesc(Long ownerId);

    Optional<Garment> findByIdAndOwnerId(Long id, Long ownerId);

    long countByOwnerId(Long ownerId);

    long countByOwnerIdAndFavoriteTrue(Long ownerId);
}
