package com.ryvenca.image;

import java.nio.file.Path;

/** Binary storage for media files. The local implementation can later be swapped for S3/GCS. */
public interface MediaStorage {

    void write(String fileName, byte[] content);

    void delete(String fileName);

    /** Directory served under /media/** (local storage only). */
    Path root();
}
