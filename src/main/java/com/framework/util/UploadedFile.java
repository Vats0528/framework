package com.framework.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class UploadedFile {
    private final String fieldName;
    private final String fileName;
    private final String contentType;
    private final byte[] content;

    public UploadedFile(String fieldName, String fileName, String contentType, byte[] content) {
        this.fieldName = fieldName;
        this.fileName = fileName;
        this.contentType = contentType;
        this.content = content;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getContent() {
        return content;
    }

    public static byte[] readAllBytes(InputStream in) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = in.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        return baos.toByteArray();
    }
}
