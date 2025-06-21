package com.gianmarco.securenotes;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.MasterKey;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.UUID;

public class SecureFileManager {
    private static final String TAG = "SecureFileManager";
    private static final String SECURE_FILES_DIR = "secure_files";
    
    private final Context context;
    private final File secureFilesDirectory;
    private final MasterKey masterKey;

    public SecureFileManager(Context context) throws Exception {
        this.context = context.getApplicationContext();
        this.masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
        
        // Crea la directory per i file cifrati
        this.secureFilesDirectory = new File(context.getFilesDir(), SECURE_FILES_DIR);
        if (!secureFilesDirectory.exists()) {
            secureFilesDirectory.mkdirs();
        }
    }

    /**
     * Salva un file in modo cifrato
     * @param inputUri URI del file da salvare
     * @param originalFileName Nome originale del file
     * @return ID univoco del file salvato
     */
    public String saveSecureFile(Uri inputUri, String originalFileName) throws IOException, GeneralSecurityException {
        String fileId = generateFileId();
        File encryptedFile = new File(secureFilesDirectory, fileId);
        
        InputStream inputStream = null;
        OutputStream outputStream = null;
        
        try {
            inputStream = context.getContentResolver().openInputStream(inputUri);
            if (inputStream == null) {
                throw new IOException("Cannot open input stream");
            }
            
            EncryptedFile encryptedFileObj = new EncryptedFile.Builder(
                    context,
                    encryptedFile,
                    masterKey,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build();
            
            outputStream = encryptedFileObj.openFileOutput();
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            Log.d(TAG, "File saved securely: " + originalFileName + " -> " + fileId);
            return fileId;
            
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.w(TAG, "Error closing input stream", e);
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.w(TAG, "Error closing output stream", e);
                }
            }
        }
    }

    /**
     * Carica un file cifrato
     * @param fileId ID del file da caricare
     * @return InputStream del file decifrato
     */
    public InputStream loadSecureFile(String fileId) throws IOException, GeneralSecurityException {
        File encryptedFile = new File(secureFilesDirectory, fileId);
        
        if (!encryptedFile.exists()) {
            throw new IOException("File not found: " + fileId);
        }
        
        EncryptedFile encryptedFileObj = new EncryptedFile.Builder(
                context,
                encryptedFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build();
        
        return encryptedFileObj.openFileInput();
    }

    /**
     * Elimina un file cifrato
     * @param fileId ID del file da eliminare
     */
    public void deleteSecureFile(String fileId) {
        File encryptedFile = new File(secureFilesDirectory, fileId);
        if (encryptedFile.exists()) {
            if (encryptedFile.delete()) {
                Log.d(TAG, "File deleted: " + fileId);
            } else {
                Log.w(TAG, "Failed to delete file: " + fileId);
            }
        }
    }

    /**
     * Verifica se un file esiste
     * @param fileId ID del file da verificare
     * @return true se il file esiste
     */
    public boolean fileExists(String fileId) {
        File encryptedFile = new File(secureFilesDirectory, fileId);
        return encryptedFile.exists();
    }

    /**
     * Ottiene la dimensione di un file
     * @param fileId ID del file
     * @return dimensione in byte, -1 se il file non esiste
     */
    public long getFileSize(String fileId) {
        File encryptedFile = new File(secureFilesDirectory, fileId);
        return encryptedFile.exists() ? encryptedFile.length() : -1;
    }

    /**
     * Genera un ID univoco per il file
     */
    private String generateFileId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Ottiene la directory dei file cifrati (per debug)
     */
    public File getSecureFilesDirectory() {
        return secureFilesDirectory;
    }
} 