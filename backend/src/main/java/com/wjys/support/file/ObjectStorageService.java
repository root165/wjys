package com.wjys.support.file;

public interface ObjectStorageService {
    void store(String objectKey, byte[] content, String contentType);
    byte[] read(String objectKey);
    void delete(String objectKey);
}
