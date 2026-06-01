package org.example.moomyeongso.domain.postimage.storage;

public interface ImageStorageService {

    StoredImageObject upload(byte[] bytes, String key, String contentType);

    void delete(String key);
}

