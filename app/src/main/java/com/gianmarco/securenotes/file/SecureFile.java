package com.gianmarco.securenotes;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "secure_files")
public class SecureFile {
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private String fileId; // ID univoco del file cifrato
    private String originalFileName; // Nome originale del file
    private String mimeType; // Tipo MIME del file
    private long fileSize; // Dimensione del file in byte
    private long uploadDate; // Data di caricamento
    private String noteId; // ID della nota a cui è allegato (opzionale)

    // Costruttori
    public SecureFile() {}

    @Ignore
    public SecureFile(String fileId, String originalFileName, String mimeType, long fileSize) {
        this.fileId = fileId;
        this.originalFileName = originalFileName;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.uploadDate = System.currentTimeMillis();
    }

    // Getters e Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(long uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getNoteId() {
        return noteId;
    }

    public void setNoteId(String noteId) {
        this.noteId = noteId;
    }

    // Metodi di utilità
    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }

    public boolean isImage() {
        return mimeType != null && mimeType.startsWith("image/");
    }

    public boolean isPdf() {
        return mimeType != null && mimeType.equals("application/pdf");
    }

    public boolean isDocument() {
        return mimeType != null && mimeType.startsWith("text/");
    }
} 