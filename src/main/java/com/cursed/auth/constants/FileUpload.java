package com.cursed.auth.constants;

import java.util.Set;

public final class FileUpload {
    private FileUpload() {
        super();
    }

    public static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024; // 5MB

    public static final Set<String> ALLOWED_IMAGE_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    public static final String IMAGE_CONTENT_TYPE_PREFIX = "image/";
}
