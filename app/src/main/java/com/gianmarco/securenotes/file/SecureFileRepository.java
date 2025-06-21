package com.gianmarco.securenotes.file;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.gianmarco.securenotes.SecureNoteDB;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SecureFileRepository {
    private static final String TAG = "SecureFileRepository";
    
    private final SecureFileDao secureFileDao;
    private final SecureFileManager secureFileManager;
    private final ExecutorService executorService;
    private final Context context;

    public SecureFileRepository(Context context) throws Exception {
        this.context = context.getApplicationContext();
        SecureNoteDB db = SecureNoteDB.getInstance(this.context);
        this.secureFileDao = db.secureFileDao();
        this.secureFileManager = new SecureFileManager(this.context);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Carica un file e lo salva in modo cifrato
     */
    public void uploadFile(Uri fileUri, String originalFileName, String mimeType, String noteId) {
        executorService.execute(() -> {
            try {
                String fileId = secureFileManager.saveSecureFile(fileUri, originalFileName);
                long fileSize = secureFileManager.getFileSize(fileId);
                SecureFile secureFile = new SecureFile(fileId, originalFileName, mimeType, fileSize);
                secureFile.setNoteId(noteId);
                secureFileDao.insert(secureFile);
                
                Log.d(TAG, "File uploaded successfully: " + originalFileName);
                
            } catch (Exception e) {
                Log.e(TAG, "Error uploading file: " + e.getMessage());
            }
        });
    }

    /**
     * Carica un file cifrato
     */
    public InputStream loadFile(String fileId) throws IOException, java.security.GeneralSecurityException {
        return secureFileManager.loadSecureFile(fileId);
    }

    /**
     * Elimina un file
     */
    public void deleteFile(SecureFile secureFile) {
        executorService.execute(() -> {
            try {
                secureFileManager.deleteSecureFile(secureFile.getFileId());
                secureFileDao.delete(secureFile);
                
                Log.d(TAG, "File deleted successfully: " + secureFile.getOriginalFileName());
                
            } catch (Exception e) {
                Log.e(TAG, "Error deleting file: " + e.getMessage());
            }
        });
    }

    /**
     * Elimina tutti i file di una nota
     */
    public void deleteFilesByNoteId(String noteId) {
        executorService.execute(() -> {
            try {
                List<SecureFile> files = secureFileDao.getFilesByNoteId(noteId).getValue();
                if (files != null) {
                    for (SecureFile file : files) {
                        secureFileManager.deleteSecureFile(file.getFileId());
                    }
                }

                secureFileDao.deleteFilesByNoteId(noteId);
                
                Log.d(TAG, "All files deleted for note: " + noteId);
                
            } catch (Exception e) {
                Log.e(TAG, "Error deleting files for note: " + e.getMessage());
            }
        });
    }

    /**
     * Ottieni tutti i file
     */
    public LiveData<List<SecureFile>> getAllFiles() {
        return secureFileDao.getAllFiles();
    }

    /**
     * Ottieni i file di una nota specifica
     */
    public LiveData<List<SecureFile>> getFilesByNoteId(String noteId) {
        return secureFileDao.getFilesByNoteId(noteId);
    }

    /**
     * Ottieni un file specifico
     */
    public LiveData<SecureFile> getFileById(long id) {
        return secureFileDao.getFileById(id);
    }

    /**
     * Ottieni il conteggio dei file
     */
    public LiveData<Integer> getFileCount() {
        return secureFileDao.getFileCount();
    }

    /**
     * Ottieni il conteggio dei file di una nota
     */
    public LiveData<Integer> getFileCountByNoteId(String noteId) {
        return secureFileDao.getFileCountByNoteId(noteId);
    }

    /**
     * Ottieni la dimensione totale dei file
     */
    public LiveData<Long> getTotalFileSize() {
        return secureFileDao.getTotalFileSize();
    }

    /**
     * Verifica se un file esiste
     */
    public boolean fileExists(String fileId) {
        return secureFileManager.fileExists(fileId);
    }

    public List<SecureFile> getAllFilesSync() {
        return secureFileDao.getAllFilesSync();
    }

    public void uploadFileSync(Uri fileUri, String originalFileName, String mimeType, String noteId) {
        try {
            String fileId = secureFileManager.saveSecureFile(fileUri, originalFileName);
            long fileSize = secureFileManager.getFileSize(fileId);
            SecureFile secureFile = new SecureFile(fileId, originalFileName, mimeType, fileSize);
            secureFile.setNoteId(noteId);
            secureFileDao.insert(secureFile);
            Log.d(TAG, "File uploaded successfully (sync): " + originalFileName);
        } catch (Exception e) {
            Log.e(TAG, "Error uploading file (sync): " + e.getMessage());
        }
    }
} 