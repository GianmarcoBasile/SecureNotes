package com.gianmarco.securenotes.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.gianmarco.securenotes.file.SecureFile;
import com.gianmarco.securenotes.file.SecureFileRepository;
import java.util.List;

public class ArchiveViewModel extends ViewModel {
    private final SecureFileRepository fileRepository;
    private final LiveData<List<SecureFile>> filesLiveData;

    public ArchiveViewModel(SecureFileRepository fileRepository) {
        this.fileRepository = fileRepository;
        this.filesLiveData = fileRepository.getAllFiles();
    }

    public LiveData<List<SecureFile>> getFiles() {
        return filesLiveData;
    }

    public void deleteFile(com.gianmarco.securenotes.file.SecureFile file) {
        fileRepository.deleteFile(file);
    }

    public void uploadFile(android.net.Uri fileUri, String originalFileName, String mimeType) {
        fileRepository.uploadFile(fileUri, originalFileName, mimeType);
    }

    public java.io.InputStream loadFile(String fileId) throws java.io.IOException, java.security.GeneralSecurityException {
        return fileRepository.loadFile(fileId);
    }
} 