package com.capsule.services.uploader.web;

import com.capsule.services.uploader.io.StorageException;
import com.capsule.services.uploader.io.StorageService;
import com.capsule.services.uploader.model.UploadRequest;
import com.capsule.services.uploader.model.UploadResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class UploaderController {
    private final StorageService storageService;

    @Autowired
    public UploaderController(StorageService service) {
        this.storageService = service;
    }

    public ResponseEntity<UploadResponse> upload(@RequestParam("file") MultipartFile file,
            @RequestParam("uuid") String uuid, @RequestParam("filename") String fileName,
            @RequestParam(value = "part_index", required = false, defaultValue = "-1") int partIndex,
            @RequestParam(value = "total_parts", required = false, defaultValue = "-1") int totalParts,
            @RequestParam(value = "total_file_size", required = false, defaultValue = "-1") long totalFileSize) {
        UploadRequest request = new UploadRequest(uuid, file);
        request.setFileName(fileName);
        request.setTotalFileSize(totalFileSize);
        request.setPartIndex(partIndex);
        request.setTotalParts(totalParts);

        storageService.save(request);
        return ResponseEntity.ok().body(new UploadResponse(true));
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<UploadResponse> handleException(StorageException ex) {
        var response = new UploadResponse(false, ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @CrossOrigin
    @DeleteMapping("/uploads/{uuid}")
    public ResponseEntity<Void> delete(@PathVariable("uuid") String uuid) {
        storageService.delete(uuid);
        return ResponseEntity.ok().build();
    }

    @CrossOrigin
    @PostMapping("/chunksdone")
    public ResponseEntity<Void> chunksDone(
        @RequestParam("uuid") String uuid,
        @RequestParam("filename") String fileName,
        @RequestParam(value = "total_parts") int totalParts,
        @RequestParam(value = "total_file_size") long totalFileSize
    ) {
        storageService.mergeChunks(uuid, fileName, totalParts, totalFileSize);
        return ResponseEntity.noContent().build();
    }
}
