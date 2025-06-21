package com.gianmarco.securenotes.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.gianmarco.securenotes.note.Note;
import com.gianmarco.securenotes.note.NoteRepository;
import java.util.List;

public class DashboardViewModel extends ViewModel {
    private final NoteRepository noteRepository;
    private final LiveData<List<Note>> notesLiveData;

    public DashboardViewModel(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
        this.notesLiveData = noteRepository.getAllNotes();
    }

    public LiveData<List<Note>> getNotes() {
        return notesLiveData;
    }

    public void deleteNote(Note note) {
        noteRepository.delete(note.getId());
    }
} 