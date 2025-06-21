package com.gianmarco.securenotes.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.gianmarco.securenotes.note.Note;
import com.gianmarco.securenotes.note.NoteRepository;

public class EditorViewModel extends ViewModel {
    private final NoteRepository noteRepository;
    private final MutableLiveData<Note> noteLiveData = new MutableLiveData<>();

    public EditorViewModel(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    public LiveData<Note> getNote() {
        return noteLiveData;
    }

    public void loadNoteById(long noteId) {
        noteRepository.getNoteById(noteId).observeForever(note -> {
            noteLiveData.setValue(note);
        });
    }

    public void saveNote(Note note) {
        noteRepository.insertOrUpdate(note);
    }
} 