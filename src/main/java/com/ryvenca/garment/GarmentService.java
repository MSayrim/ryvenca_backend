package com.ryvenca.garment;

import java.util.EnumSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ryvenca.color.ColorName;
import com.ryvenca.color.ColorScience;
import com.ryvenca.common.ApiException;
import com.ryvenca.garment.GarmentDtos.GarmentRequest;
import com.ryvenca.image.ImageAsset;
import com.ryvenca.image.ImageService;
import com.ryvenca.outfit.SavedOutfitRepository;

@Service
public class GarmentService {

    private final GarmentRepository garments;
    private final ImageService images;
    private final SavedOutfitRepository savedOutfits;

    public GarmentService(GarmentRepository garments, ImageService images, SavedOutfitRepository savedOutfits) {
        this.garments = garments;
        this.images = images;
        this.savedOutfits = savedOutfits;
    }

    @Transactional(readOnly = true)
    public List<Garment> wardrobe(long ownerId) {
        return garments.findByOwnerIdOrderByCreatedAtDescIdDesc(ownerId);
    }

    @Transactional(readOnly = true)
    public List<Garment> list(long ownerId, GarmentFilter filter) {
        return wardrobe(ownerId).stream().filter(filter::matches).toList();
    }

    @Transactional(readOnly = true)
    public Garment get(long ownerId, long id) {
        return garments.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> ApiException.notFound("Parça bulunamadı."));
    }

    @Transactional
    public Garment create(long ownerId, GarmentRequest request) {
        if (request.imageId() == null) {
            throw ApiException.invalidField("imageId", "Önce kıyafetinin fotoğrafını ekle.");
        }
        ImageAsset image = images.requireOwned(ownerId, request.imageId());
        if (image.isAttached()) {
            throw ApiException.invalidField("imageId", "Bu fotoğraf zaten başka bir parçada kullanılıyor.");
        }
        Garment garment = new Garment(ownerId);
        image.setAttached(true);
        garment.setImage(image);
        apply(garment, request, true);
        return garments.save(garment);
    }

    @Transactional
    public Garment update(long ownerId, long id, GarmentRequest request) {
        Garment garment = get(ownerId, id);
        boolean imageChanged = false;
        if (request.imageId() != null && !request.imageId().equals(garment.getImage().getId())) {
            ImageAsset replacement = images.requireOwned(ownerId, request.imageId());
            if (replacement.isAttached()) {
                throw ApiException.invalidField("imageId", "Bu fotoğraf zaten başka bir parçada kullanılıyor.");
            }
            ImageAsset old = garment.getImage();
            replacement.setAttached(true);
            garment.setImage(replacement);
            garments.flush();
            images.delete(old);
            imageChanged = true;
        }
        boolean colorChanged = request.color() != garment.getColor();
        apply(garment, request, imageChanged || colorChanged);
        return garment;
    }

    @Transactional
    public Garment setFavorite(long ownerId, long id, boolean favorite) {
        Garment garment = get(ownerId, id);
        garment.setFavorite(favorite);
        return garment;
    }

    @Transactional
    public void delete(long ownerId, long id) {
        Garment garment = get(ownerId, id);
        savedOutfits.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .filter(saved -> saved.garmentIds().contains(id))
                .forEach(savedOutfits::delete);
        ImageAsset image = garment.getImage();
        garments.delete(garment);
        garments.flush();
        images.delete(image);
    }

    @Transactional(readOnly = true)
    public long favoriteCount(long ownerId) {
        return garments.countByOwnerIdAndFavoriteTrue(ownerId);
    }

    private void apply(Garment garment, GarmentRequest request, boolean resolveColor) {
        if (request.subcategory().category() != request.category()) {
            throw ApiException.invalidField("subcategory", "Alt kategori seçilen kategoriye ait değil.");
        }
        garment.setType(request.subcategory());
        String name = request.name() == null ? null : request.name().trim();
        garment.setName(name == null || name.isEmpty() ? null : name);
        garment.setPattern(Boolean.TRUE.equals(request.pattern()));
        garment.setSeasons(request.seasons() == null || request.seasons().isEmpty()
                ? request.subcategory().defaultSeasons()
                : EnumSet.copyOf(request.seasons()));
        garment.setOccasions(request.occasions() == null || request.occasions().isEmpty()
                ? request.subcategory().defaultOccasions()
                : EnumSet.copyOf(request.occasions()));
        if (resolveColor) {
            resolveColor(garment, request.color(), request.colorHex());
        }
    }

    /**
     * Keeps the measured tone when the user accepts the detected color; a manual override uses the
     * hex the client sent (if any) or the canonical swatch of the chosen color.
     */
    private static void resolveColor(Garment garment, ColorName color, String requestedHex) {
        ImageAsset image = garment.getImage();
        if (color == image.getDetectedColor() && image.getDetectedHex() != null) {
            garment.setColor(color, image.getDetectedHex(), ColorSource.AUTO);
        } else if (ColorScience.isHex(requestedHex)) {
            String hex = requestedHex.startsWith("#") ? requestedHex : "#" + requestedHex;
            garment.setColor(color, hex.toUpperCase(java.util.Locale.ROOT), ColorSource.MANUAL);
        } else {
            garment.setColor(color, color.hex(), ColorSource.MANUAL);
        }
    }
}
