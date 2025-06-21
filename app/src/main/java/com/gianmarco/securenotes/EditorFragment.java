package com.gianmarco.securenotes;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

public class EditorFragment extends Fragment {

    private static final String ARG_NOTE_ID = "note_id";
    private static final long INVALID_NOTE_ID = -1;

    private EditText titleEditText;
    private EditText contentEditText;
    private NoteRepository noteRepository;
    private long currentNoteId = INVALID_NOTE_ID;
    private Note currentNote = null;

    public static EditorFragment newInstance(long noteId) {
        EditorFragment fragment = new EditorFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_NOTE_ID, noteId);
        fragment.setArguments(args);
        return fragment;
    }

    public static EditorFragment newInstance() {
        return newInstance(INVALID_NOTE_ID);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        noteRepository = new NoteRepository(requireContext());
        if (getArguments() != null) {
            currentNoteId = getArguments().getLong(ARG_NOTE_ID, INVALID_NOTE_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        titleEditText = view.findViewById(R.id.edit_text_title);
        contentEditText = view.findViewById(R.id.editTextNote);
        Button saveButton = view.findViewById(R.id.btnSaveNote);

        if (currentNoteId != INVALID_NOTE_ID) {
            noteRepository.getNoteById(currentNoteId).observe(getViewLifecycleOwner(), note -> {
                if (note != null) {
                    currentNote = note;
                    titleEditText.setText(note.getTitle());
                    contentEditText.setText(note.getContent());
                }
            });
        }

        saveButton.setOnClickListener(v -> saveNote());
    }

    private void saveNote() {
        String title = titleEditText.getText().toString().trim();
        String content = contentEditText.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            Toast.makeText(getContext(), "Title cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentNote == null) {
            currentNote = new Note();
        }

        currentNote.setTitle(title);
        currentNote.setContent(content);
        currentNote.setLastModified(System.currentTimeMillis());

        noteRepository.insertOrUpdate(currentNote);

        Toast.makeText(getContext(), "Note saved", Toast.LENGTH_SHORT).show();
        getParentFragmentManager().popBackStack();
    }
} 