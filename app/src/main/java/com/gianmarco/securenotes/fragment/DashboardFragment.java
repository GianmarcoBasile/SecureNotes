package com.gianmarco.securenotes.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import com.gianmarco.securenotes.MainActivity;
import com.gianmarco.securenotes.note.Note;
import com.gianmarco.securenotes.note.NoteRepository;
import com.gianmarco.securenotes.R;
import com.gianmarco.securenotes.adapter.NoteAdapter;
import com.gianmarco.securenotes.viewmodel.DashboardViewModel;

public class DashboardFragment extends Fragment implements NoteAdapter.OnNoteClickListener, NoteAdapter.OnNoteDeleteListener {

    private NoteAdapter noteAdapter;
    private DashboardViewModel viewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        NoteRepository noteRepository = new NoteRepository(requireContext());
        viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            public <T extends androidx.lifecycle.ViewModel> T create(Class<T> modelClass) {
                return (T) new DashboardViewModel(noteRepository);
            }
        }).get(DashboardViewModel.class);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_notes);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        ViewCompat.setOnApplyWindowInsetsListener(recyclerView, (v, insets) -> {
            int bottomNavHeightPx = (int) (160 * v.getResources().getDisplayMetrics().density);
            int systemBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    Math.max(bottomNavHeightPx, systemBottom)
            );
            return insets;
        });

        noteAdapter = new NoteAdapter(new ArrayList<>(), this, this);
        recyclerView.setAdapter(noteAdapter);

        viewModel.getNotes().observe(getViewLifecycleOwner(), notes -> {
            if (notes != null) {
                noteAdapter.updateNotes(notes);
            }
        });
    }

    @Override
    public void onNoteClick(Note note) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideBottomNavAndFab();
        }

        EditorFragment editorFragment = EditorFragment.newInstance(note.getId());
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, editorFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onNoteDelete(Note note) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Elimina nota")
                .setMessage("Sei sicuro di voler eliminare la nota '" + note.getTitle() + "'?")
                .setPositiveButton("Elimina", (dialog, which) -> {
                    viewModel.deleteNote(note);
                })
                .setNegativeButton("Annulla", null)
                .show();
    }
} 