package com.gianmarco.securenotes;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;

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
                // Se c'è un errore di database, prova a resettarlo
                handleDatabaseError();
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
                handleDatabaseError();
            }
        });
    }

    public List<Note> getAllNotesSync() {
        return noteDao.getAllNotesSync();
    }

    private void handleDatabaseError() {
        try {
            Log.w(TAG, "Attempting to reset database due to error...");
            SecureNoteDB.resetDatabase(context);
        } catch (Exception e) {
            Log.e(TAG, "Failed to reset database: " + e.getMessage());
        }
    }
} 