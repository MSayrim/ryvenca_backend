package com.ryvenca.image;

import java.util.List;
import java.util.UUID;

import com.ryvenca.color.ColorName;

public final class ImageDtos {

    private ImageDtos() {
    }

    public record ColorCandidate(ColorName color, String hex, double score) {
    }

    public record Detection(ColorName color, String hex, double confidence, boolean patternLikely,
                            List<ColorCandidate> candidates) {
    }

    public record ImageUpload(UUID imageId, String imageUrl, String thumbnailUrl, int width, int height,
                              Detection detection) {
    }
}
