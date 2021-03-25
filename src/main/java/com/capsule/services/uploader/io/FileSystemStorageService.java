package com.capsule.services.uploader.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import com.capsule.services.uploader.UploadServerProperties;
import com.capsule.services.uploader.model.UploadRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

@Service
public class FileSystemStorageService implements StorageService {
    
    private static final Logger log = LoggerFactory.getLogger(FileSystemStorageService.class);

    private Path basePath;

    @Autowired
    public FileSystemStorageService(UploadServerProperties props) {
        this.basePath = props.getBaseDir();
    }

    @Override
    public void save(UploadRequest request) {
        if (request.getFile().isEmpty()) {
            throw new StorageException(String.format("File with uuid = [%s] is empty", request.getUuid().toString()));
        }
        
        Path targetFile;
        if (request.getPartIndex() > -1) {
            targetFile = basePath.resolve(request.getUuid()).resolve(String.format("%s_%05d", request.getUuid(),request.getPartIndex()));
        } else {
            targetFile = basePath.resolve(request.getUuid()).resolve(request.getFileName());
        }
        try {
            Files.createDirectories(targetFile.getParent());
            Files.copy(request.getFile().getInputStream(), targetFile);
        } catch (IOException ex) {
            String errorMsg = String.format("Error occured when saving file with uuid = [%s]", request);
            log.error(errorMsg, ex);
            throw new StorageException(errorMsg, ex);
        }
    }

    @Override
    public void delete(String uuid) {
        File targetDir = basePath.resolve(uuid).toFile();        
        FileSystemUtils.deleteRecursively(targetDir);
    }

    @Override
    public void mergeChunks(String uuid, String fileName, int totalParts, long totalFileSize) {        
        var targetFile = basePath.resolve(uuid).resolve(fileName).toFile();
        try (FileChannel dest = new FileOutputStream(targetFile, true).getChannel()) {
            for (int i = 0; i < totalParts; i++) {
                var sourceFile = basePath.resolve(uuid).resolve(String.format("%s_%05d", uuid, i)).toFile();
                try (FileChannel src = new FileInputStream(sourceFile).getChannel()) {
                    dest.position(dest.size());
                    src.transferTo(0, src.size(), dest);
                }
                sourceFile.delete();
            }
        } catch (IOException ex) { 
            String errorMsg = String.format("Error occured when merging chunks for uuid = [%s]", uuid);
            log.error(errorMsg, ex);
            throw new StorageException(errorMsg, ex);
        }
    }

}
