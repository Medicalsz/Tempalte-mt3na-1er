package com.medicare.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.File;
import java.util.Map;

public class CloudinaryService {

    private static volatile Cloudinary client;

    public static class UploadResult {
        public final String secureUrl;
        public final String publicId;

        UploadResult(String secureUrl, String publicId) {
            this.secureUrl = secureUrl;
            this.publicId = publicId;
        }
    }

    private static Cloudinary cloudinary() {
        if (client == null) {
            synchronized (CloudinaryService.class) {
                if (client == null) {
                    client = new Cloudinary(ObjectUtils.asMap(
                            "cloud_name", CloudinaryConfig.cloudName(),
                            "api_key", CloudinaryConfig.apiKey(),
                            "api_secret", CloudinaryConfig.apiSecret(),
                            "secure", true
                    ));
                }
            }
        }
        return client;
    }

    public UploadResult upload(File file) {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File missing: " + file);
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = cloudinary().uploader().upload(file, ObjectUtils.asMap(
                    "folder", CloudinaryConfig.uploadFolder(),
                    "resource_type", "image",
                    "overwrite", true
            ));
            return new UploadResult((String) res.get("secure_url"), (String) res.get("public_id"));
        } catch (Exception e) {
            throw new RuntimeException("Cloudinary upload failed: " + e.getMessage(), e);
        }
    }

    public void delete(String publicId) {
        if (publicId == null || publicId.isBlank()) return;
        try {
            cloudinary().uploader().destroy(publicId, ObjectUtils.asMap("invalidate", true));
        } catch (Exception e) {
            System.out.println("Cloudinary delete warning: " + e.getMessage());
        }
    }
}
