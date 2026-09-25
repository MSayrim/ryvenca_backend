package com.ryvenca.image;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ryvenca.config.RyvencaProperties;

@Component
public class LocalMediaStorage implements MediaStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalMediaStorage.class);

    private final Path root;

    public LocalMediaStorage(RyvencaProperties properties) {
        this.root = Path.of(properties.storage().localDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Media directory could not be created: " + root, e);
        }
    }

    @Override
    public void write(String fileName, byte[] content) {
        Path target = resolve(fileName);
        try {
            Path tmp = Files.createTempFile(root, ".upload-", ".tmp");
            Files.write(tmp, content);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("Media file could not be written: " + fileName, e);
        }
    }

    @Override
    public void delete(String fileName) {
        try {
            Files.deleteIfExists(resolve(fileName));
        } catch (IOException e) {
            log.warn("Media file could not be deleted: {}", fileName, e);
        }
    }

    @Override
    public Path root() {
        return root;
    }

    private Path resolve(String fileName) {
        Path path = root.resolve(fileName).normalize();
        if (!path.startsWith(root) || !path.getParent().equals(root)) {
            throw new IllegalArgumentException("Invalid media file name: " + fileName);
        }
        return path;
    }
}
