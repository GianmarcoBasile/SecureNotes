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

@Database(entities = {Note.class, SecureFile.class}, version = 3, exportSchema = false)
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
        try {
            Log.d(TAG, "Tentando di creare il database");
            return buildDatabase(context);
        } catch (Exception e) {
                Log.e(TAG, "Impossibile creare il database: " + e.getMessage());
                throw new RuntimeException("Could not create database", e);
            }
        }

    private static SecureNoteDB buildDatabase(Context context) {
        final byte[] passphrase = PassphraseManager.getPassphrase(context);
        final SupportFactory factory = new SupportFactory(passphrase);
        
        return Room.databaseBuilder(context.getApplicationContext(),
                        SecureNoteDB.class, DB_NAME)
                .openHelperFactory(factory)
                .fallbackToDestructiveMigration()
                .addCallback(new RoomDatabase.Callback() {
                    @Override
                    public void onCreate(androidx.sqlite.db.SupportSQLiteDatabase db) {
                        super.onCreate(db);
                        Log.d(TAG, "Database creato con successo");
                    }

                    @Override
                    public void onOpen(androidx.sqlite.db.SupportSQLiteDatabase db) {
                        super.onOpen(db);
                        Log.d(TAG, "Database aperto con successo");
                    }
                })
                .build();
    }
} 