package com.gianmarco.securenotes;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class ArchivePinManager {
    private static final String TAG = "ArchivePinManager";
    private static final String PREFS_NAME = "archive_pin_prefs";
    private static final String KEY_ARCHIVE_PIN_ENABLED = "archive_pin_enabled";
    private static final String KEY_ARCHIVE_PIN_HASH = "archive_pin_hash";

    private final Context context;
    private final SharedPreferences encryptedPrefs;

    public ArchivePinManager(Context context) throws GeneralSecurityException, IOException {
        this.context = context.getApplicationContext();
        
        // Crea una chiave master per cifrare le SharedPreferences
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        this.encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }

    /**
     * Verifica se il PIN dell'archivio è abilitato
     */
    public boolean isArchivePinEnabled() {
        return encryptedPrefs.getBoolean(KEY_ARCHIVE_PIN_ENABLED, false);
    }

    /**
     * Abilita o disabilita il PIN dell'archivio
     */
    public void setArchivePinEnabled(boolean enabled) {
        encryptedPrefs.edit().putBoolean(KEY_ARCHIVE_PIN_ENABLED, enabled).apply();
        Log.d(TAG, "Archive PIN enabled: " + enabled);
    }

    /**
     * Imposta il PIN dell'archivio (viene salvato come hash)
     */
    public void setArchivePin(String pin) {
        if (pin == null || pin.trim().isEmpty()) {
            throw new IllegalArgumentException("PIN non può essere vuoto");
        }
        
        // Crea un hash del PIN per sicurezza
        String pinHash = createPinHash(pin);
        encryptedPrefs.edit()
                .putString(KEY_ARCHIVE_PIN_HASH, pinHash)
                .putBoolean(KEY_ARCHIVE_PIN_ENABLED, true)
                .apply();
        
        Log.d(TAG, "Archive PIN impostato");
    }

    /**
     * Verifica se il PIN fornito è corretto
     */
    public boolean verifyArchivePin(String pin) {
        if (!isArchivePinEnabled()) {
            return true; // Se non è abilitato, sempre vero
        }
        
        if (pin == null || pin.trim().isEmpty()) {
            return false;
        }
        
        String storedHash = encryptedPrefs.getString(KEY_ARCHIVE_PIN_HASH, null);
        if (storedHash == null) {
            return false;
        }
        
        String inputHash = createPinHash(pin);
        boolean isValid = storedHash.equals(inputHash);
        
        Log.d(TAG, "Archive PIN verification: " + isValid);
        return isValid;
    }

    /**
     * Rimuove il PIN dell'archivio
     */
    public void removeArchivePin() {
        encryptedPrefs.edit()
                .remove(KEY_ARCHIVE_PIN_HASH)
                .putBoolean(KEY_ARCHIVE_PIN_ENABLED, false)
                .apply();
        
        Log.d(TAG, "Archive PIN rimosso");
    }

    /**
     * Crea un hash semplice del PIN per sicurezza
     */
    private String createPinHash(String pin) {
        // Implementazione semplice - in produzione usare algoritmi più sicuri
        int hash = pin.hashCode();
        return String.valueOf(hash);
    }
} 