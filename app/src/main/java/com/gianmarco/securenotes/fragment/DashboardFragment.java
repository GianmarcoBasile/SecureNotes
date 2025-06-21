package com.gianmarco.securenotes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import com.gianmarco.securenotes.adapter.NoteAdapter;

public class DashboardFragment extends Fragment {

    private NoteAdapter noteAdapter;
    private NoteRepository noteRepository;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        noteRepository = new NoteRepository(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_notes);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        noteAdapter = new NoteAdapter(new ArrayList<>(), this::onNoteClicked, this::onNoteDelete);
        recyclerView.setAdapter(noteAdapter);

        noteRepository.getAllNotes().observe(getViewLifecycleOwner(), notes -> {
            if (notes != null) {
                noteAdapter.updateNotes(notes);
            }
        });
    }

    private void onNoteClicked(Note note) {
        // Nascondi il menu e il FAB prima di aprire l'editor
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideBottomNavAndFab();
        }

        // Navigate to EditorFragment with the note
        EditorFragment editorFragment = EditorFragment.newInstance(note.getId());
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, editorFragment)
                .addToBackStack(null)
                .commit();
    }

    private void onNoteDelete(Note note) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Elimina nota")
                .setMessage("Sei sicuro di voler eliminare la nota '" + note.getTitle() + "'?")
                .setPositiveButton("Elimina", (dialog, which) -> {
                    noteRepository.delete(note.getId());
                })
                .setNegativeButton("Annulla", null)
                .show();
    }
} 