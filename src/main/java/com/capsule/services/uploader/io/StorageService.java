package com.capsule.services.uploader.io;

import com.capsule.services.uploader.model.UploadRequest;

public interface StorageService {
    void save(UploadRequest uploadRequest);
    
    void delete(String uuid);

    void mergeChunks(String uuid, String fileName, int totalParts, long totalFileSize);
}
