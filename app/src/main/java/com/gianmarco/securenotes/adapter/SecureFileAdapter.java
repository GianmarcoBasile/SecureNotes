package com.gianmarco.securenotes.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gianmarco.securenotes.R;
import com.gianmarco.securenotes.file.SecureFile;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SecureFileAdapter extends RecyclerView.Adapter<SecureFileAdapter.SecureFileViewHolder> {
    
    private List<SecureFile> files = new ArrayList<>();
    private final OnFileClickListener onFileClickListener;
    private final OnFileDeleteListener onFileDeleteListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public interface OnFileClickListener {
        void onFileClick(SecureFile secureFile);
    }

    public interface OnFileDeleteListener {
        void onFileDelete(SecureFile secureFile);
    }

    public SecureFileAdapter(OnFileClickListener onFileClickListener, OnFileDeleteListener onFileDeleteListener) {
        this.onFileClickListener = onFileClickListener;
        this.onFileDeleteListener = onFileDeleteListener;
    }

    @NonNull
    @Override
    public SecureFileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_secure_file, parent, false);
        return new SecureFileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SecureFileViewHolder holder, int position) {
        holder.bind(files.get(position));
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public void updateFiles(List<SecureFile> newFiles) {
        this.files = newFiles != null ? newFiles : new ArrayList<>();
        notifyDataSetChanged();
    }

    class SecureFileViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageFileType;
        private final TextView textFileName;
        private final TextView textFileInfo;
        private final ImageButton buttonDelete;

        public SecureFileViewHolder(@NonNull View itemView) {
            super(itemView);
            imageFileType = itemView.findViewById(R.id.image_file_type);
            textFileName = itemView.findViewById(R.id.text_file_name);
            textFileInfo = itemView.findViewById(R.id.text_file_info);
            buttonDelete = itemView.findViewById(R.id.button_delete);

            // Click sul file per visualizzarlo
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onFileClickListener != null) {
                    onFileClickListener.onFileClick(files.get(position));
                }
            });

            // Click sul bottone elimina
            buttonDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onFileDeleteListener != null) {
                    onFileDeleteListener.onFileDelete(files.get(position));
                }
            });
        }

        public void bind(SecureFile secureFile) {
            textFileName.setText(secureFile.getOriginalFileName());
            
            // Imposta l'icona in base al tipo di file
            if (secureFile.isImage()) {
                imageFileType.setImageResource(android.R.drawable.ic_menu_gallery);
            } else if (secureFile.isPdf()) {
                imageFileType.setImageResource(android.R.drawable.ic_menu_view);
            } else if (secureFile.isDocument()) {
                imageFileType.setImageResource(android.R.drawable.ic_menu_edit);
            } else {
                imageFileType.setImageResource(android.R.drawable.ic_menu_help);
            }

            // Formatta le informazioni del file
            String fileInfo = String.format("%s • %s • %s",
                    getFileTypeDescription(secureFile.getMimeType()),
                    secureFile.getFormattedFileSize(),
                    dateFormat.format(new Date(secureFile.getUploadDate()))
            );
            textFileInfo.setText(fileInfo);
        }

        private String getFileTypeDescription(String mimeType) {
            if (mimeType == null) return "File";
            
            if (mimeType.startsWith("image/")) {
                return "Immagine";
            } else if (mimeType.equals("application/pdf")) {
                return "PDF";
            } else if (mimeType.startsWith("text/")) {
                return "Testo";
            } else if (mimeType.contains("document")) {
                return "Documento";
            } else if (mimeType.contains("word")) {
                return "Word";
            } else if (mimeType.contains("excel")) {
                return "Excel";
            } else if (mimeType.contains("powerpoint")) {
                return "PowerPoint";
            } else {
                return "File";
            }
        }
    }
} 