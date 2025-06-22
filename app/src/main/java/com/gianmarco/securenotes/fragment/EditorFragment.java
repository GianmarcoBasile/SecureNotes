package com.gianmarco.securenotes.fragment;

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

import com.gianmarco.securenotes.note.Note;
import com.gianmarco.securenotes.note.NoteRepository;
import com.gianmarco.securenotes.R;
import com.gianmarco.securenotes.viewmodel.EditorViewModel;

public class EditorFragment extends Fragment {

    private static final String ARG_NOTE_ID = "note_id";
    private static final long INVALID_NOTE_ID = -1;

    private EditText titleEditText;
    private EditText contentEditText;
    private NoteRepository noteRepository;
    private long currentNoteId = INVALID_NOTE_ID;
    private Note currentNote = null;
    private EditorViewModel viewModel;

    public static EditorFragment newInstance(long noteId) {
        EditorFragment fragment = new EditorFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_NOTE_ID, noteId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NoteRepository noteRepository = new NoteRepository(requireContext());
        viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @Override
            public <T extends androidx.lifecycle.ViewModel> T create(Class<T> modelClass) {
                return (T) new EditorViewModel(noteRepository);
            }
        }).get(EditorViewModel.class);
        if (getArguments() != null) {
            currentNoteId = getArguments().getLong(ARG_NOTE_ID, INVALID_NOTE_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_editor, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        titleEditText = view.findViewById(R.id.edit_text_title);
        contentEditText = view.findViewById(R.id.editTextNote);
        Button saveButton = view.findViewById(R.id.btnSaveNote);

        if (currentNoteId != INVALID_NOTE_ID) {
            viewModel.getNoteById(currentNoteId).observe(getViewLifecycleOwner(), note -> {
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
            Toast.makeText(getContext(), "Il titolo non può essere vuoto", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentNote == null) {
            currentNote = new Note();
        }

        currentNote.setTitle(title);
        currentNote.setContent(content);
        currentNote.setLastModified(System.currentTimeMillis());

        viewModel.saveNote(currentNote);

        Toast.makeText(getContext(), "Nota salvata", Toast.LENGTH_SHORT).show();
        getParentFragmentManager().popBackStack();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof com.gianmarco.securenotes.MainActivity) {
            ((com.gianmarco.securenotes.MainActivity) getActivity()).hideBottomNavAndFab();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() instanceof com.gianmarco.securenotes.MainActivity) {
            ((com.gianmarco.securenotes.MainActivity) getActivity()).showBottomNavAndFab();
        }
    }
} 