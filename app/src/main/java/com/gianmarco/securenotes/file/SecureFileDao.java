package com.gianmarco.securenotes.file;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SecureFileDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SecureFile secureFile);
    
    @Update
    void update(SecureFile secureFile);
    
    @Delete
    void delete(SecureFile secureFile);
    
    @Query("SELECT * FROM secure_files ORDER BY uploadDate DESC")
    LiveData<List<SecureFile>> getAllFiles();
    
    @Query("SELECT * FROM secure_files WHERE id = :id")
    LiveData<SecureFile> getFileById(long id);
    
    @Query("SELECT * FROM secure_files WHERE fileId = :fileId")
    SecureFile getFileByFileId(String fileId);
    
    @Query("SELECT COUNT(*) FROM secure_files")
    LiveData<Integer> getFileCount();
    
    @Query("SELECT SUM(fileSize) FROM secure_files")
    LiveData<Long> getTotalFileSize();
    
    @Query("SELECT * FROM secure_files ORDER BY uploadDate DESC")
    List<SecureFile> getAllFilesSync();
} 