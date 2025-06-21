package com.gianmarco.securenotes.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gianmarco.securenotes.note.Note;
import com.gianmarco.securenotes.R;

import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private List<Note> notes;
    private final OnNoteClickListener listener;
    private final OnNoteDeleteListener deleteListener;

    public interface OnNoteClickListener {
        void onNoteClick(Note note);
    }

    public interface OnNoteDeleteListener {
        void onNoteDelete(Note note);
    }

    public NoteAdapter(List<Note> notes, OnNoteClickListener listener, OnNoteDeleteListener deleteListener) {
        this.notes = notes;
        this.listener = listener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notes.get(position);
        holder.bind(note, listener, deleteListener);
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    public void updateNotes(List<Note> newNotes) {
        this.notes.clear();
        this.notes.addAll(newNotes);
        notifyDataSetChanged();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView contentPreviewTextView;
        ImageButton deleteButton;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.text_view_title);
            contentPreviewTextView = itemView.findViewById(R.id.text_view_content_preview);
            deleteButton = itemView.findViewById(R.id.button_delete_note);
        }

        public void bind(final Note note, final OnNoteClickListener listener, final OnNoteDeleteListener deleteListener) {
            titleTextView.setText(note.getTitle());
            contentPreviewTextView.setText(note.getContent());
            itemView.setOnClickListener(v -> listener.onNoteClick(note));
            deleteButton.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onNoteDelete(note);
                }
            });
        }
    }
} 