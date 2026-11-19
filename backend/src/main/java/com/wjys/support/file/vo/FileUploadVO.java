package com.wjys.support.file.vo;

public record FileUploadVO(Long fileId, String uploadToken, String checksum, String originalName) { }
