package com.gianmarco.securenotes;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

public class PassphraseManager {

    private static final String PREFS_FILE = "passphrase_prefs";
    private static final String KEY_PASSPHRASE = "db_passphrase";

    // Genera e salva una passphrase sicura se non esiste
    public static byte[] getOrCreatePassphrase(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                    context,
                    PREFS_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            String base64Passphrase = sharedPreferences.getString(KEY_PASSPHRASE, null);
            if (base64Passphrase == null) {
                byte[] passphraseBytes = new byte[32];
                new SecureRandom().nextBytes(passphraseBytes);
                base64Passphrase = Base64.encodeToString(passphraseBytes, Base64.NO_WRAP);
                sharedPreferences.edit().putString(KEY_PASSPHRASE, base64Passphrase).apply();
                return passphraseBytes;
            }
            
            return Base64.decode(base64Passphrase, Base64.NO_WRAP);

        } catch (Exception e) {
            throw new RuntimeException("Could not get or create passphrase", e);
        }
    }
} 