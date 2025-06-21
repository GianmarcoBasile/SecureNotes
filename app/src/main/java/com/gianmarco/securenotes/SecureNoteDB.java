package com.gianmarco.securenotes;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.gianmarco.securenotes.file.SecureFile;
import com.gianmarco.securenotes.file.SecureFileDao;
import com.gianmarco.securenotes.note.Note;
import com.gianmarco.securenotes.note.NoteDao;

import net.sqlcipher.database.SupportFactory;

import java.io.File;

@Database(entities = {Note.class, SecureFile.class}, version = 2, exportSchema = false)
public abstract class SecureNoteDB extends RoomDatabase {

    private static final String TAG = "SecureNoteDB";
    private static final String DB_NAME = "secure_notes.db";
    
    public abstract NoteDao noteDao();
    public abstract SecureFileDao secureFileDao();

    private static volatile SecureNoteDB INSTANCE;

    public static SecureNoteDB getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (SecureNoteDB.class) {
                if (INSTANCE == null) {
                    INSTANCE = createDatabase(context);
                }
            }
        }
        return INSTANCE;
    }

    private static SecureNoteDB createDatabase(Context context) {
        // Prima prova a creare/aprire il database normalmente
        try {
            Log.d(TAG, "Attempting to create/open database normally...");
            return buildDatabase(context);
        } catch (Exception e) {
            Log.e(TAG, "Error creating/opening database: " + e.getMessage());
            
            // Se fallisce, elimina il database corrotto e ricrea
            try {
                Log.w(TAG, "Database appears to be corrupted, recreating...");
                cleanupCorruptedDatabase(context);
                return buildDatabase(context);
            } catch (Exception e2) {
                Log.e(TAG, "Failed to recreate database: " + e2.getMessage());
                throw new RuntimeException("Could not create database", e2);
            }
        }
    }

    private static void cleanupCorruptedDatabase(Context context) {
        try {
            // Chiudi l'istanza esistente se presente
            if (INSTANCE != null) {
                INSTANCE.close();
                INSTANCE = null;
            }
            
            // Elimina il database corrotto
            context.deleteDatabase(DB_NAME);
            
            // Elimina anche eventuali file correlati che potrebbero essere corrotti
            File dbFile = context.getDatabasePath(DB_NAME);
            if (dbFile.exists()) {
                dbFile.delete();
            }
            
            // Elimina file di backup e journal se esistono
            File dbFileWAL = new File(dbFile.getPath() + "-wal");
            if (dbFileWAL.exists()) {
                dbFileWAL.delete();
            }
            
            File dbFileSHM = new File(dbFile.getPath() + "-shm");
            if (dbFileSHM.exists()) {
                dbFileSHM.delete();
            }
            
            Log.d(TAG, "Corrupted database files cleaned up successfully");
        } catch (Exception e) {
            Log.w(TAG, "Error during database cleanup: " + e.getMessage());
        }
    }

    private static SecureNoteDB buildDatabase(Context context) {
        final byte[] passphrase = PassphraseManager.getOrCreatePassphrase(context);
        final SupportFactory factory = new SupportFactory(passphrase);
        
        return Room.databaseBuilder(context.getApplicationContext(),
                        SecureNoteDB.class, DB_NAME)
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .addCallback(new RoomDatabase.Callback() {
                    @Override
                    public void onCreate(androidx.sqlite.db.SupportSQLiteDatabase db) {
                        super.onCreate(db);
                        Log.d(TAG, "Database created successfully");
                    }

                    @Override
                    public void onOpen(androidx.sqlite.db.SupportSQLiteDatabase db) {
                        super.onOpen(db);
                        Log.d(TAG, "Database opened successfully");
                    }
                })
                .build();
    }

    // Metodo per forzare la ricreazione del database (utile per debug)
    public static void resetDatabase(Context context) {
        synchronized (SecureNoteDB.class) {
            cleanupCorruptedDatabase(context);
            Log.d(TAG, "Database reset completed");
        }
    }
} 