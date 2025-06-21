package com.gianmarco.securenotes;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.gianmarco.securenotes.note.Note;
import com.gianmarco.securenotes.note.NoteRepository;
import com.gianmarco.securenotes.file.SecureFileRepository;
import com.gianmarco.securenotes.file.SecureFileManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.CipherInputStream;

public class RestoreBackupWorker extends Worker {
    private static final String TAG = "RestoreBackupWorker";

    public RestoreBackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String uriString = getInputData().getString("restore_uri");
        String password = getInputData().getString("restore_password");
        if (uriString == null || password == null || password.length() < 6) return Result.failure();
        Uri srcUri = Uri.parse(uriString);
        Context context = getApplicationContext();
        int notesImported = 0;
        int filesImported = 0;
        try {
            Log.d(TAG, "Apro lo stream del file di backup: " + uriString);
            // Leggi salt e IV dal file
            try (InputStream in = context.getContentResolver().openInputStream(srcUri)) {
                if (in == null) throw new Exception("Impossibile aprire il file di backup");
                byte[] salt = new byte[16];
                byte[] iv = new byte[16];
                if (in.read(salt) != 16 || in.read(iv) != 16) throw new Exception("Backup corrotto (salt/iv)");
                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 10000, 256);
                SecretKey tmp = factory.generateSecret(spec);
                SecretKeySpec secret = new SecretKeySpec(tmp.getEncoded(), "AES");
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, secret, new javax.crypto.spec.IvParameterSpec(iv));
                try (CipherInputStream cipherIn = new CipherInputStream(in, cipher)) {
                    ZipInputStream zipIn = new ZipInputStream(cipherIn);
                    ZipEntry entry;
                    byte[] buffer = new byte[4096];
                    NoteRepository noteRepo = new NoteRepository(context);
                    SecureFileRepository fileRepo = new SecureFileRepository(context);
                    SecureFileManager fileManager = new SecureFileManager(context);
                    boolean foundNotes = false;
                    while ((entry = zipIn.getNextEntry()) != null) {
                        Log.d(TAG, "Entry trovata nello zip: " + entry.getName());
                        if (entry.getName().equals("notes.json")) {
                            foundNotes = true;
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            int len;
                            while ((len = zipIn.read(buffer)) != -1) baos.write(buffer, 0, len);
                            String notesJson = baos.toString("UTF-8");
                            Log.d(TAG, "Contenuto notes.json: " + notesJson);
                            try {
                                JSONObject obj = new JSONObject(notesJson);
                                JSONArray notesArr = obj.getJSONArray("notes");
                                for (int i = 0; i < notesArr.length(); i++) {
                                    JSONObject n = notesArr.getJSONObject(i);
                                    Note note = new Note();
                                    note.setTitle(n.getString("title"));
                                    note.setContent(n.getString("content"));
                                    note.setLastModified(n.getLong("lastModified"));
                                    noteRepo.insertOrUpdate(note);
                                    notesImported++;
                                }
                                Log.d(TAG, "Note importate: " + notesImported);
                            } catch (Exception e) {
                                Log.e(TAG, "Errore parsing note: ", e);
                                showNotification(context, "Ripristino SecureNotes", "Errore parsing notes.json: " + notesJson, false);
                                throw new Exception("Il backup è corrotto o il formato non è compatibile (note)");
                            }
                        } else if (entry.getName().startsWith("files/")) {
                            try {
                                String fileName = entry.getName().substring("files/".length());
                                Log.d(TAG, "Inizio import file: " + fileName);
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                int len;
                                while ((len = zipIn.read(buffer)) != -1) baos.write(buffer, 0, len);
                                byte[] fileBytes = baos.toByteArray();
                                File temp = File.createTempFile("import_", fileName, context.getCacheDir());
                                FileOutputStream fos = new FileOutputStream(temp);
                                fos.write(fileBytes);
                                fos.close();
                                Uri fileUri = androidx.core.content.FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", temp);
                                Log.d(TAG, "Chiamo fileRepo.uploadFileSync per: " + fileName);
                                fileRepo.uploadFileSync(fileUri, fileName, getMimeType(fileName), null);
                                temp.delete();
                                filesImported++;
                                Log.d(TAG, "File importato: " + fileName);
                            } catch (Exception e) {
                                Log.e(TAG, "Errore import file: ", e);
                                throw new Exception("Il backup è corrotto o il formato non è compatibile (file)");
                            }
                        }
                        zipIn.closeEntry();
                    }
                    zipIn.close();
                    if (!foundNotes || notesImported == 0) {
                        throw new Exception("Nessuna nota importata: backup corrotto o formato non compatibile");
                    }
                    Log.i(TAG, "Ripristino backup completato: note importate=" + notesImported + ", file importati=" + filesImported);
                    showNotification(context, "Ripristino SecureNotes", "Backup importato con successo", true);
                    return Result.success();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Errore durante il ripristino: ", e);
            showNotification(context, "Ripristino SecureNotes", "Errore durante il ripristino: " + e.getMessage(), false);
            return Result.failure();
        }
    }

    private String getMimeType(String fileName) {
        String mimeType = java.net.URLConnection.guessContentTypeFromName(fileName);
        return mimeType != null ? mimeType : "application/octet-stream";
    }

    private void showNotification(Context context, String title, String message, boolean success) {
        String channelId = "restore_channel";
        android.app.NotificationChannel channel = new android.app.NotificationChannel(
                channelId,
                "Ripristino",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
        );
        android.app.NotificationManager manager = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(success ? android.R.drawable.stat_sys_upload_done : android.R.drawable.stat_notify_error)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        androidx.core.app.NotificationManagerCompat notificationManager = androidx.core.app.NotificationManagerCompat.from(context);
        notificationManager.notify(2001, builder.build());
    }
} 