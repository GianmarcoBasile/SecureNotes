package com.gianmarco.securenotes.note;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.lifecycle.LiveData;

import java.util.List;

@Dao
public interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Note note);

    @Update
    void update(Note note);

    @Query("SELECT * FROM notes ORDER BY lastModified DESC")
    LiveData<List<Note>> getAllNotes();

    @Query("SELECT * FROM notes WHERE id = :noteId")
    LiveData<Note> getNoteById(long noteId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(Note note);

    @Query("DELETE FROM notes WHERE id = :noteId")
    void deleteById(long noteId);

    @Query("SELECT * FROM notes ORDER BY lastModified DESC")
    List<Note> getAllNotesSync();

} 