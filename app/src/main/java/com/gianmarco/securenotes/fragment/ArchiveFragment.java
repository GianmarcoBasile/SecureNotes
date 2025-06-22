package com.gianmarco.securenotes.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.lifecycle.ViewModelProvider;

import com.gianmarco.securenotes.ArchivePinManager;
import com.gianmarco.securenotes.MainActivity;
import com.gianmarco.securenotes.R;
import com.gianmarco.securenotes.file.SecureFile;
import com.gianmarco.securenotes.adapter.SecureFileAdapter;
import com.gianmarco.securenotes.file.SecureFileRepository;
import com.gianmarco.securenotes.viewmodel.SettingsViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import com.gianmarco.securenotes.viewmodel.ArchiveViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class ArchiveFragment extends Fragment implements SecureFileAdapter.OnFileClickListener, SecureFileAdapter.OnFileDeleteListener {

    private RecyclerView recyclerView;
    private TextView textEmptyState;
    private FloatingActionButton fabAddFile;
    private SecureFileAdapter fileAdapter;
    private SecureFileRepository fileRepository;
    private ArchivePinManager archivePinManager;
    private boolean archiveUnlocked = false;
    private ArchiveViewModel viewModel;
    private SettingsViewModel settingsViewModel;

    // ActivityResultLauncher per la selezione dei file
    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri fileUri = result.getData().getData();
                    if (fileUri != null) {
                        handleFileSelection(fileUri);
                    }
                }
            }
    );

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            fileRepository = new SecureFileRepository(requireContext());
            archivePinManager = new ArchivePinManager(requireContext());
            viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
                public <T extends androidx.lifecycle.ViewModel> T create(Class<T> modelClass) {
                    return (T) new ArchiveViewModel(fileRepository);
                }
            }).get(ArchiveViewModel.class);

            settingsViewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
                public <T extends androidx.lifecycle.ViewModel> T create(Class<T> modelClass) {
                    return (T) new SettingsViewModel(archivePinManager);
                }
            }).get(SettingsViewModel.class);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Errore nell'inizializzazione dell'archivio", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_archive, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_view_files);
        textEmptyState = view.findViewById(R.id.text_empty_state);
        fabAddFile = view.findViewById(R.id.fab_add_file);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        fileAdapter = new SecureFileAdapter(new ArrayList<>(), this, this);
        recyclerView.setAdapter(fileAdapter);

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

        if (archivePinManager != null && settingsViewModel.isArchivePinEnabled() && !archiveUnlocked) {
            showArchivePinDialog();
        } else {
            setupArchiveContent();
        }
    }

    private void setupArchiveContent() {
        viewModel.getFiles().observe(getViewLifecycleOwner(), files -> {
            if (files != null) {
                fileAdapter.updateFiles(files);
                updateEmptyState(files.isEmpty());
            }
        });
    }

    private void showArchivePinDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("PIN Archivio");
        builder.setMessage("Inserisci il PIN per accedere all'archivio");

        EditText pinInput = new EditText(requireContext());
        pinInput.setHint("PIN");
        pinInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        builder.setView(pinInput);

        builder.setPositiveButton("Sblocca", (dialog, which) -> {
            String pin = pinInput.getText().toString();
            if (settingsViewModel.verifyPin(pin)) {
                archiveUnlocked = true;
                setupArchiveContent();
                Toast.makeText(requireContext(), "Archivio sbloccato", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "PIN non corretto", Toast.LENGTH_SHORT).show();
                showArchivePinDialog();
            }
        });

        builder.setNegativeButton("Annulla", (dialog, which) -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).getBottomNavigationView().setSelectedItemId(R.id.nav_notes);
            }
        });

        builder.setCancelable(false);
        builder.show();
    }

    public void showFileTypeMenu() {
        String[] options = {"Immagine", "PDF", "Documenti"};
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Seleziona tipo di file");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Immagine
                    pickFile("image/*");
                    break;
                case 1: // PDF
                    pickFile("application/pdf");
                    break;
                case 2: // Documenti (solo file di testo)
                    pickFile("text/*");
                    break;
            }
        });
        builder.show();
    }

    private void pickFile(String mimeType) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(mimeType);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(Intent.createChooser(intent, "Seleziona file"));
    }

    private void handleFileSelection(Uri fileUri) {
        try {
            String fileName = getFileName(fileUri);
            String mimeType = getMimeTypeFromExtension(fileName);
            
            if (fileName == null) {
                fileName = "File_" + System.currentTimeMillis();
            }

            viewModel.uploadFile(fileUri, fileName, mimeType);
            Snackbar.make(requireView(), "File caricato con successo", Snackbar.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("ArchiveFragment", "Errore nella gestione della selezione del file: " + e.getMessage(), e);
            Snackbar.make(requireView(), "Errore nel caricamento del file", Snackbar.LENGTH_LONG).show();
        }
    }

    private String getMimeTypeFromExtension(String fileName) {
        String mimeType = java.net.URLConnection.guessContentTypeFromName(fileName);
        return mimeType != null ? mimeType : "application/octet-stream";
    }

    private String getFileName(Uri uri) {
        String result = null;
        try (android.database.Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    result = cursor.getString(index);
                }
            }
        } catch (Exception e) {
            Log.e("ArchiveFragment", "Errore nella query del nome del file: " + e.getMessage());
        }

        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            textEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            textEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onFileClick(SecureFile secureFile) {
        try {
            InputStream inputStream = viewModel.loadFile(secureFile.getFileId());
            if (secureFile.isImage()) {
                showImageFile(secureFile, inputStream);
            } else {
                // File temporaneo dal flusso decifrato
                String originalFileName = secureFile.getOriginalFileName();
                String extension = getFileExtension(originalFileName);
                String nameWithoutExt = originalFileName.contains(".") ? originalFileName.substring(0, originalFileName.lastIndexOf('.')) : originalFileName;
                String timestamp = String.valueOf(System.currentTimeMillis());
                File tempFile = new File(requireContext().getCacheDir(), nameWithoutExt + "_" + timestamp + extension);
    
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
                inputStream.close();
                openFileWithExternalApp(tempFile, originalFileName);
            }
        } catch (Exception e) {
            Log.e("ArchiveFragment", "Errore nell'apertura del file: " + e.getMessage());
            Toast.makeText(requireContext(), "Errore nell'apertura del file", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isTextDocument(SecureFile file) {
        String mime = file.getMimeType();
        String fileName = file.getOriginalFileName().toLowerCase();
        
        if (mime != null && mime.startsWith("text/")) {
            return true;
        }
        
        // Controlla estensioni comuni per file di testo
        String[] textExtensions = {
            ".txt", ".md", ".csv", ".xml", ".json", ".html", ".htm", 
            ".css", ".js", ".log", ".ini", ".cfg", ".conf", ".yml", ".yaml"
        };
        
        for (String ext : textExtensions) {
            if (fileName.endsWith(ext)) {
                return true;
            }
        }
        
        return false;
    }

    private void showImageFile(SecureFile secureFile, InputStream inputStream) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle(secureFile.getOriginalFileName());
        
        // Crea un ImageView per mostrare l'immagine
        ImageView imageView = new ImageView(requireContext());
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setAdjustViewBounds(true);
        
        int padding = (int) (16 * requireContext().getResources().getDisplayMetrics().density);
        imageView.setPadding(padding, padding, padding, padding);
        
        // Carica l'immagine dall'InputStream
        try {
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
            imageView.setImageBitmap(bitmap);
            inputStream.close();
        } catch (Exception e) {
            Log.e("ArchiveFragment", "Errore nel caricamento dell'immagine: " + e.getMessage());
            Toast.makeText(requireContext(), "Errore nel caricamento dell'immagine", Toast.LENGTH_SHORT).show();
            return;
        }
        
        builder.setView(imageView);
        builder.setPositiveButton("Chiudi", null);
        builder.show();
    }

    private void openFileWithExternalApp(File tempFile, String originalFileName) {
        try {
            Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    tempFile
            );
    
            String mainMimeType = getMimeTypeFromExtension(originalFileName);
    
            ArrayList<String> mimeTypes = new ArrayList<>();
            mimeTypes.add(mainMimeType);
            if (mainMimeType.startsWith("text/")) {
                mimeTypes.add("text/plain");
                mimeTypes.add("application/octet-stream");
            } else if (mainMimeType.equals("application/pdf")) {
            } else {
                mimeTypes.add("application/octet-stream");
            }
    
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Intent.EXTRA_SUBJECT, originalFileName);
    
            boolean opened = false;
            for (String mimeType : mimeTypes) {
                intent.setDataAndType(fileUri, mimeType);
                if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                    startActivity(intent);
                    scheduleFileDeletion(tempFile, 3000);
                    opened = true;
                    break;
                }
            }
    
            if (!opened) {
                tempFile.delete();
                Toast.makeText(requireContext(), "Nessuna app trovata per aprire il file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("ArchiveFragment", "Errore nell'apertura del file con app esterna: " + e.getMessage());
            Toast.makeText(requireContext(), "Errore nell'apertura del file", Toast.LENGTH_SHORT).show();
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return "." + fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    @Override
    public void onFileDelete(SecureFile secureFile) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Elimina file")
                .setMessage("Sei sicuro di voler eliminare il file '" + secureFile.getOriginalFileName() + "'?")
                .setPositiveButton("Elimina", (dialog, which) -> {
                    viewModel.deleteFile(secureFile);
                })
                .setNegativeButton("Annulla", null)
                .show();
    }

    private void scheduleFileDeletion(File file, long delayMillis) {
        new Handler().postDelayed(() -> {
            if (file.exists()) {
                file.delete();
            }
        }, delayMillis);
    }
} 