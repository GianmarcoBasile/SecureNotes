package com.gianmarco.securenotes;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.gianmarco.securenotes.file.SecureFile;
import com.gianmarco.securenotes.file.SecureFileRepository;
import com.gianmarco.securenotes.note.Note;
import com.gianmarco.securenotes.note.NoteRepository;

public class BackupWorker extends Worker {
    private static final String TAG = "BackupWorker";
    private static final int PBKDF2_ITERATIONS = 10000;
    private static final int KEY_LENGTH = 256;
    private static final int IV_LENGTH = 16;
    private static final int SALT_LENGTH = 16;

    public BackupWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @Override
    public Result doWork() {
        String uriString = getInputData().getString("backup_uri");
        if (uriString == null) return Result.failure();
        Uri destUri = Uri.parse(uriString);
        String password = getInputData().getString("backup_password");
        if (password == null || password.length() < 6) return Result.failure();
        Context context = getApplicationContext();
        try {
            // Estrai tutte le note
            NoteRepository noteRepo = new NoteRepository(context);
            List<Note> notes = noteRepo.getAllNotesSync();
            if (notes == null) notes = java.util.Collections.emptyList();
            JSONArray notesArray = new JSONArray();
            for (Note n : notes) {
                JSONObject obj = new JSONObject();
                obj.put("id", n.getId());
                obj.put("title", n.getTitle());
                obj.put("content", n.getContent());
                obj.put("lastModified", n.getLastModified());
                notesArray.put(obj);
            }
            JSONObject notesJson = new JSONObject();
            notesJson.put("notes", notesArray);
            byte[] notesBytes = notesJson.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

            // Estrai tutti i file
            SecureFileRepository fileRepo = new SecureFileRepository(context);
            List<SecureFile> files = fileRepo.getAllFilesSync();
            if (files == null) files = java.util.Collections.emptyList();

            // Crea zip in memoria
            ByteArrayOutputStream zipBaos = new ByteArrayOutputStream();
            ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(zipBaos));
            // Aggiungi le note
            zipOut.putNextEntry(new ZipEntry("notes.json"));
            zipOut.write(notesBytes);
            zipOut.closeEntry();
            // Aggiungi i file
            for (SecureFile f : files) {
                try (InputStream is = fileRepo.loadFile(f.getFileId())) {
                    zipOut.putNextEntry(new ZipEntry("files/" + f.getOriginalFileName()));
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        zipOut.write(buffer, 0, len);
                    }
                    zipOut.closeEntry();
                    Log.d(TAG, "File aggiunto al backup: " + f.getOriginalFileName());
                } catch (Exception e) {
                    Log.w(TAG, "Errore nel backup file: " + f.getOriginalFileName(), e);
                }
            }
            zipOut.close();
            byte[] zipBytes = zipBaos.toByteArray();

            // Cifra lo zip con AES/PBKDF2
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secret = new SecretKeySpec(tmp.getEncoded(), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secret, new IvParameterSpec(iv));
            try (OutputStream out = context.getContentResolver().openOutputStream(destUri)) {
                if (out == null) throw new Exception("Impossibile aprire l'output stream");
                out.write(salt);
                out.write(iv);
                try (CipherOutputStream cipherOut = new CipherOutputStream(out, cipher)) {
                    cipherOut.write(zipBytes);
                }
            }
            Log.i(TAG, "Backup completato con successo (AES/PBKDF2)");
            showNotification(context, "Backup SecureNotes", "Backup completato con successo", true);
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Errore durante il backup: ", e);
            showNotification(context, "Backup SecureNotes", "Errore durante il backup", false);
            return Result.failure();
        }
    }

    private void showNotification(Context context, String title, String message, boolean success) {
        String channelId = "backup_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Backup",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(success ? android.R.drawable.stat_sys_upload_done : android.R.drawable.stat_notify_error)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(1001, builder.build());
    }
} 