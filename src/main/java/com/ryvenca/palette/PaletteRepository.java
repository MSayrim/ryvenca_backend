package com.ryvenca.palette;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaletteRepository extends JpaRepository<Palette, String> {

    List<Palette> findAllByOrderBySortOrderAscIdAsc();
}
