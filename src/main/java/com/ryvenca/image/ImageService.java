package com.ryvenca.image;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ryvenca.color.ColorDetection;
import com.ryvenca.color.DominantColorDetector;
import com.ryvenca.common.ApiException;
import com.ryvenca.common.ErrorCode;
import com.ryvenca.config.RyvencaProperties;
import com.ryvenca.image.ImageDtos.ColorCandidate;
import com.ryvenca.image.ImageDtos.Detection;
import com.ryvenca.image.ImageDtos.ImageUpload;

@Service
public class ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);

    private final ImageRepository images;
    private final ImageProcessor processor;
    private final DominantColorDetector detector;
    private final MediaStorage storage;
    private final MediaUrls urls;
    private final RyvencaProperties properties;

    public ImageService(ImageRepository images, ImageProcessor processor, DominantColorDetector detector,
                        MediaStorage storage, MediaUrls urls, RyvencaProperties properties) {
        this.images = images;
        this.processor = processor;
        this.detector = detector;
        this.storage = storage;
        this.urls = urls;
        this.properties = properties;
    }

    @Transactional
    public ImageUpload upload(long ownerId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_IMAGE, "Lütfen bir fotoğraf seç.");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ApiException(ErrorCode.INVALID_IMAGE, "Fotoğraf yüklenemedi. Lütfen tekrar dene.");
        }
        BufferedImage decoded = processor.decode(bytes);
        ColorDetection detection = detector.detect(decoded);

        UUID id = UUID.randomUUID();
        BufferedImage display = processor.display(decoded);
        String originalFile = id + "-orig.jpg";
        String displayFile = id + "-display.jpg";
        String thumbFile = id + "-thumb.jpg";
        storage.write(originalFile, processor.toJpeg(processor.original(decoded), 0.9f));
        storage.write(displayFile, processor.toJpeg(display, 0.85f));
        storage.write(thumbFile, processor.toJpeg(processor.thumbnail(display, detection.focusX(), detection.focusY()), 0.82f));

        ImageAsset asset = new ImageAsset(id, ownerId, originalFile, displayFile, thumbFile, display.getWidth(),
                display.getHeight());
        asset.setDetection(detection.color(), detection.measuredHex(), detection.confidence(), detection.patternLikely());
        images.save(asset);

        List<ColorCandidate> candidates = detection.candidates().stream()
                .map(m -> new ColorCandidate(m.color(), m.color().hex(), round(m.score())))
                .toList();
        return new ImageUpload(id, url(asset), thumbnailUrl(asset), asset.getWidth(), asset.getHeight(),
                new Detection(detection.color(), detection.measuredHex(), round(detection.confidence()),
                        detection.patternLikely(), candidates));
    }

    @Transactional(readOnly = true)
    public ImageAsset requireOwned(long ownerId, UUID imageId) {
        return images.findByIdAndOwnerId(imageId, ownerId)
                .orElseThrow(() -> ApiException.invalidField("imageId", "Fotoğraf bulunamadı. Lütfen tekrar yükle."));
    }

    public String url(ImageAsset asset) {
        return urls.url(asset.getDisplayFile());
    }

    public String thumbnailUrl(ImageAsset asset) {
        return urls.url(asset.getThumbFile());
    }

    @Transactional
    public void delete(ImageAsset asset) {
        deleteFiles(asset);
        images.delete(asset);
    }

    @Transactional
    public void deleteAllFilesOf(long ownerId) {
        images.findByOwnerId(ownerId).forEach(this::deleteFiles);
    }

    /** Uploaded photos that never became a garment are removed after the retention period. */
    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT5M")
    @Transactional
    public void purgeDrafts() {
        Instant cutoff = Instant.now().minus(properties.storage().draftRetention());
        List<ImageAsset> drafts = images.findByAttachedFalseAndCreatedAtBefore(cutoff);
        drafts.forEach(this::deleteFiles);
        images.deleteAll(drafts);
        if (!drafts.isEmpty()) {
            log.info("Purged {} draft images", drafts.size());
        }
    }

    private void deleteFiles(ImageAsset asset) {
        storage.delete(asset.getOriginalFile());
        storage.delete(asset.getDisplayFile());
        storage.delete(asset.getThumbFile());
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }
}
