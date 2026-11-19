package com.wjys.support.file;

import com.wjys.common.model.ApiResponse;
import com.wjys.support.file.dto.BindFilesRequest;
import com.wjys.support.file.vo.FileUploadVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FileController {
    private final FileService fileService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileUploadVO> upload(@RequestPart("file") MultipartFile file) { return ApiResponse.success(fileService.upload(file)); }

    @PostMapping("/bind")
    public ApiResponse<Void> bind(@Valid @RequestBody BindFilesRequest request) { fileService.bindAll(request.fileIds(), request.bizType(), request.bizId()); return ApiResponse.success(null); }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) { return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(fileService.read(id)); }
}
