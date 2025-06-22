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

    public boolean isArchivePinEnabled() {
        return encryptedPrefs.getBoolean(KEY_ARCHIVE_PIN_ENABLED, false);
    }

    public void setArchivePinEnabled(boolean enabled) {
        encryptedPrefs.edit().putBoolean(KEY_ARCHIVE_PIN_ENABLED, enabled).apply();
        Log.d(TAG, "PIN archivio:" + enabled);
    }

    public void setArchivePin(String pin) {
        if (pin == null || pin.trim().isEmpty()) {
            throw new IllegalArgumentException("Il PIN non può essere vuoto");
        }
        
        String pinHash = createPinHash(pin);
        encryptedPrefs.edit()
                .putString(KEY_ARCHIVE_PIN_HASH, pinHash)
                .putBoolean(KEY_ARCHIVE_PIN_ENABLED, true)
                .apply();
        
        Log.d(TAG, "PIN archivio impostato");
    }

    public boolean verifyArchivePin(String pin) {
        if (!isArchivePinEnabled()) {
            return true;
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
        
        Log.d(TAG, "Verifica PIN archivio: " + isValid);
        return isValid;
    }

    private String createPinHash(String pin) {
        int hash = pin.hashCode();
        return String.valueOf(hash);
    }
} 