package com.gianmarco.securenotes;

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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gianmarco.securenotes.adapter.SecureFileAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class ArchiveFragment extends Fragment implements SecureFileAdapter.OnFileClickListener, SecureFileAdapter.OnFileDeleteListener {

    private RecyclerView recyclerView;
    private TextView textEmptyState;
    private FloatingActionButton fabAddFile;
    private SecureFileAdapter fileAdapter;
    private SecureFileRepository fileRepository;
    private ArchivePinManager archivePinManager;
    private boolean archiveUnlocked = false;

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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            fileRepository = new SecureFileRepository(requireContext());
            archivePinManager = new ArchivePinManager(requireContext());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Errore nell'inizializzazione dell'archivio", Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_archive, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_view_files);
        textEmptyState = view.findViewById(R.id.text_empty_state);
        fabAddFile = view.findViewById(R.id.fab_add_file);

        // Debug: verifica che il FAB sia trovato
        if (fabAddFile == null) {
            Log.e("ArchiveFragment", "FAB not found!");
        } else {
            Log.d("ArchiveFragment", "FAB found and setting up listener");
        }

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        fileAdapter = new SecureFileAdapter(this, this);
        recyclerView.setAdapter(fileAdapter);

        // Il FAB è ora gestito globalmente da MainActivity
        // Rimuoviamo il listener locale per evitare conflitti
        fabAddFile.setVisibility(View.GONE); // Nascondiamo il FAB locale

        // Verifica se è necessario il PIN per l'archivio
        if (archivePinManager != null && archivePinManager.isArchivePinEnabled() && !archiveUnlocked) {
            showArchivePinDialog();
        } else {
            setupArchiveContent();
        }
    }

    private void setupArchiveContent() {
        // Osserva i file
        if (fileRepository != null) {
            fileRepository.getAllFiles().observe(getViewLifecycleOwner(), files -> {
                if (files != null) {
                    fileAdapter.updateFiles(files);
                    updateEmptyState(files.isEmpty());
                }
            });
        }
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
            if (archivePinManager.verifyArchivePin(pin)) {
                archiveUnlocked = true;
                setupArchiveContent();
                Toast.makeText(requireContext(), "Archivio sbloccato", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "PIN non corretto", Toast.LENGTH_SHORT).show();
                // Richiedi di nuovo il PIN
                showArchivePinDialog();
            }
        });

        builder.setNegativeButton("Annulla", (dialog, which) -> {
            // Torna alla sezione note
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).getBottomNavigationView().setSelectedItemId(R.id.nav_notes);
            }
        });

        builder.setCancelable(false);
        builder.show();
    }

    public void showFileTypeMenu() {
        String[] options = {"Immagine", "PDF", "Documenti", "Tutti i file"};
        
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
                case 3: // Tutti i file
                    pickFile("*/*");
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
            
            Log.d("ArchiveFragment", "File selected: " + fileName);
            Log.d("ArchiveFragment", "Detected MIME type: " + mimeType);
            
            if (fileName == null) {
                fileName = "File_" + System.currentTimeMillis();
            }

            // Carica il file
            fileRepository.uploadFile(fileUri, fileName, mimeType, null);
            
            Snackbar.make(requireView(), "File caricato con successo", Snackbar.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e("ArchiveFragment", "Error handling file selection: " + e.getMessage(), e);
            Snackbar.make(requireView(), "Errore nel caricamento del file", Snackbar.LENGTH_LONG).show();
        }
    }

    private String getMimeTypeFromExtension(String fileName) {
        String mimeType = java.net.URLConnection.guessContentTypeFromName(fileName);
        return mimeType != null ? mimeType : "application/octet-stream";
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            }
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
            InputStream inputStream = fileRepository.loadFile(secureFile.getFileId());
            if (secureFile.isImage()) {
                showImageFile(secureFile, inputStream);
            } else if (secureFile.isPdf()) {
                openPdfDirectly(secureFile, inputStream);
            } else if (isTextDocument(secureFile)) {
                openTextFileWithExternalApp(secureFile, inputStream);
            } else {
                shareFile(secureFile, inputStream);
            }
        } catch (Exception e) {
            Log.e("ArchiveFragment", "Error opening file: " + e.getMessage());
            Toast.makeText(requireContext(), "Errore nell'apertura del file", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isTextDocument(SecureFile file) {
        String mime = file.getMimeType();
        String fileName = file.getOriginalFileName().toLowerCase();
        
        // Controlla MIME type
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
        // Crea un dialog per mostrare l'immagine
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle(secureFile.getOriginalFileName());
        
        // Crea un ImageView per mostrare l'immagine
        ImageView imageView = new ImageView(requireContext());
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setAdjustViewBounds(true);
        
        // Aggiungi padding per creare spazio tra titolo e immagine
        int padding = (int) (16 * requireContext().getResources().getDisplayMetrics().density);
        imageView.setPadding(padding, padding, padding, padding);
        
        // Carica l'immagine dall'InputStream
        try {
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
            imageView.setImageBitmap(bitmap);
            inputStream.close();
        } catch (Exception e) {
            Log.e("ArchiveFragment", "Error loading image: " + e.getMessage());
            Toast.makeText(requireContext(), "Errore nel caricamento dell'immagine", Toast.LENGTH_SHORT).show();
            return;
        }
        
        builder.setView(imageView);
        builder.setPositiveButton("Chiudi", null);
        builder.show();
    }

    private void openPdfDirectly(SecureFile secureFile, InputStream inputStream) {
        try {
            // Crea un file temporaneo con il nome originale
            String originalFileName = secureFile.getOriginalFileName();
            File tempFile = new File(requireContext().getCacheDir(), originalFileName);
            
            // Se il file esiste già, aggiungi un timestamp per evitare conflitti
            if (tempFile.exists()) {
                String nameWithoutExt = originalFileName.substring(0, originalFileName.lastIndexOf('.'));
                String extension = getFileExtension(originalFileName);
                String timestamp = String.valueOf(System.currentTimeMillis());
                tempFile = new File(requireContext().getCacheDir(), nameWithoutExt + "_" + timestamp + extension);
            }
            
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
            inputStream.close();
            openPdfWithExternalApp(tempFile);
        } catch (Exception e) {
            Log.e("ArchiveFragment", "Error opening PDF: " + e.getMessage());
            Toast.makeText(requireContext(), "Errore nell'apertura del PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void openTextFileWithExternalApp(SecureFile secureFile, InputStream inputStream) {
        try {
            // Crea un file temporaneo con il nome originale
            String originalFileName = secureFile.getOriginalFileName();
            File tempFile = new File(requireContext().getCacheDir(), originalFileName);
            
            // Se il file esiste già, aggiungi un timestamp per evitare conflitti
            if (tempFile.exists()) {
                String nameWithoutExt = originalFileName.substring(0, originalFileName.lastIndexOf('.'));
                String extension = getFileExtension(originalFileName);
                String timestamp = String.valueOf(System.currentTimeMillis());
                tempFile = new File(requireContext().getCacheDir(), nameWithoutExt + "_" + timestamp + extension);
            }
            
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
            inputStream.close();
            openTextFileWithExternalApp(tempFile, originalFileName);
        } catch (Exception e) {
            Log.e("ArchiveFragment", "Error opening text file: " + e.getMessage());
            Toast.makeText(requireContext(), "Errore nell'apertura del file di testo", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareFile(SecureFile secureFile, InputStream inputStream) {
        try {
            File tempFile;
            
            if (inputStream != null) {
                // Crea un file temporaneo
                String extension = getFileExtension(secureFile.getOriginalFileName());
                tempFile = File.createTempFile("secure_file_", extension, requireContext().getCacheDir());
                
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
                inputStream.close();
            } else {
                // Il file è già stato creato, cerca il file temporaneo più recente
                String extension = getFileExtension(secureFile.getOriginalFileName());
                File cacheDir = requireContext().getCacheDir();
                File[] files = cacheDir.listFiles((dir, name) -> name.endsWith(extension));
                
                if (files != null && files.length > 0) {
                    // Usa il file più recente
                    tempFile = files[0];
                    for (File file : files) {
                        if (file.lastModified() > tempFile.lastModified()) {
                            tempFile = file;
                        }
                    }
                } else {
                    throw new IOException("File temporaneo non trovato");
                }
            }
            
            // Condividi il file
            Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    tempFile
            );
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(secureFile.getMimeType());
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, secureFile.getOriginalFileName());
            shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(shareIntent, "Condividi file"));
            
        } catch (Exception e) {
            Log.e("ArchiveFragment", "Error sharing file: " + e.getMessage());
            Toast.makeText(requireContext(), "Errore nella condivisione del file", Toast.LENGTH_SHORT).show();
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
        // Conferma eliminazione
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Elimina file");
        builder.setMessage("Sei sicuro di voler eliminare il file \"" + secureFile.getOriginalFileName() + "\"?");
        builder.setPositiveButton("Elimina", (dialog, which) -> {
            fileRepository.deleteFile(secureFile);
            Snackbar.make(requireView(), "File eliminato", Snackbar.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Annulla", null);
        builder.show();
    }

    private void openPdfWithExternalApp(File tempFile) {
        try {
            Uri pdfUri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    tempFile
            );
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(intent);
                // Elimina il file temporaneo dopo 3 secondi
                scheduleFileDeletion(tempFile, 3000);
            } else {
                // Se nessuna app è trovata, elimina immediatamente il file
                tempFile.delete();
                Toast.makeText(requireContext(), "Nessuna app trovata per visualizzare il PDF", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("ArchiveFragment", "Error opening PDF with external app: " + e.getMessage());
            Toast.makeText(requireContext(), "Errore nell'apertura del PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void openTextFileWithExternalApp(File tempFile, String originalFileName) {
        try {
            Uri textUri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    tempFile
            );
            
            // Determina il MIME type dall'estensione originale
            String mimeType = getMimeTypeFromExtension(originalFileName);
            Log.d("ArchiveFragment", "Original file: " + originalFileName + ", MIME type: " + mimeType);
            
            // Prova prima con il MIME type specifico
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(textUri, mimeType);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Intent.EXTRA_SUBJECT, originalFileName);
            
            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(intent);
                // Elimina il file temporaneo dopo 3 secondi
                scheduleFileDeletion(tempFile, 3000);
                return;
            }
            
            // Se non funziona, prova con text/plain come fallback
            Log.d("ArchiveFragment", "Specific MIME type failed, trying text/plain");
            intent.setDataAndType(textUri, "text/plain");
            
            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(intent);
                // Elimina il file temporaneo dopo 3 secondi
                scheduleFileDeletion(tempFile, 3000);
                return;
            }
            
            // Se ancora non funziona, prova con application/octet-stream
            Log.d("ArchiveFragment", "text/plain failed, trying application/octet-stream");
            intent.setDataAndType(textUri, "application/octet-stream");
            
            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(intent);
                // Elimina il file temporaneo dopo 3 secondi
                scheduleFileDeletion(tempFile, 3000);
                return;
            }
            
            // Se nessuna app è trovata, elimina immediatamente il file
            tempFile.delete();
            Toast.makeText(requireContext(), "Nessuna app trovata per visualizzare il file di testo", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e("ArchiveFragment", "Error opening text file with external app: " + e.getMessage());
            Toast.makeText(requireContext(), "Errore nell'apertura del file di testo", Toast.LENGTH_SHORT).show();
        }
    }

    private void scheduleFileDeletion(File file, long delayMillis) {
        new Handler().postDelayed(() -> {
            if (file.exists()) {
                file.delete();
            }
        }, delayMillis);
    }
} 