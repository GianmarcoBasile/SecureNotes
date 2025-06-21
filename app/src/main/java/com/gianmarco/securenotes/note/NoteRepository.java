package com.gianmarco.securenotes.note;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;

import com.gianmarco.securenotes.SecureNoteDB;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NoteRepository {
    private static final String TAG = "NoteRepository";
    
    private final NoteDao noteDao;
    private final ExecutorService executorService;
    private final Context context;

    public NoteRepository(Context context) {
        this.context = context.getApplicationContext();
        SecureNoteDB db = SecureNoteDB.getInstance(this.context);
        this.noteDao = db.noteDao();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void insertOrUpdate(Note note) {
        executorService.execute(() -> {
            try {
                noteDao.insertOrUpdate(note);
            } catch (Exception e) {
                Log.e(TAG, "Error inserting/updating note: " + e.getMessage());
            }
        });
    }

    public LiveData<List<Note>> getAllNotes() {
        return noteDao.getAllNotes();
    }

    public LiveData<Note> getNoteById(long noteId) {
        return noteDao.getNoteById(noteId);
    }

    public void delete(long noteId) {
        executorService.execute(() -> {
            try {
                noteDao.deleteById(noteId);
            } catch (Exception e) {
                Log.e(TAG, "Error deleting note: " + e.getMessage());
            }
        });
    }

    public List<Note> getAllNotesSync() {
        return noteDao.getAllNotesSync();
    }
} 