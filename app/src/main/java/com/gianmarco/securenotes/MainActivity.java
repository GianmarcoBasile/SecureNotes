package com.gianmarco.securenotes;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity {

    // Timeout di sessione in millisecondi (default 3 minuti = 180000 ms)
    private static final long DEFAULT_SESSION_TIMEOUT_MS = 3 * 60 * 1000;
    // Chiave per salvare il timestamp dell'ultima autenticazione
    private static final String PREFS_NAME = "secure_notes_prefs";
    private static final String KEY_LAST_AUTH = "last_auth_time";
    private static final String KEY_SESSION_TIMEOUT = "session_timeout";

    private boolean isAuthenticated = false; // Per evitare richieste multiple

    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fab;
    private View fragmentContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Riferimenti agli elementi dell'interfaccia
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fab = findViewById(R.id.fab_add_note);
        fragmentContainer = findViewById(R.id.fragment_container);
        
        // Nascondi completamente l'interfaccia finché non c'è autenticazione
        hideUI();
        
        // All'avvio, richiedi autenticazione
        requestAuthenticationIfNeeded();

        // Listener per la selezione delle voci del menu
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_notes) {
                fab.show();
                navigateTo(new DashboardFragment());
                return true;
            } else if (itemId == R.id.nav_archive) {
                fab.show();
                navigateTo(new ArchiveFragment());
                return true;
            } else if (itemId == R.id.nav_settings) {
                fab.hide();
                navigateTo(new SettingsFragment());
                return true;
            }
            return false;
        });

        // Listener per il click del FAB: gestisce azioni diverse in base alla sezione
        fab.setOnClickListener(v -> {
            int selectedItemId = bottomNavigationView.getSelectedItemId();
            if (selectedItemId == R.id.nav_notes) {
                // Nella sezione Note: apri EditorFragment per creare una nuova nota
                hideBottomNavAndFab();
                getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new EditorFragment())
                    .addToBackStack("editor")
                    .commit();
            } else if (selectedItemId == R.id.nav_archive) {
                // Nella sezione Archivio: mostra il menu per selezionare tipo di file
                Fragment currentFragment = getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_container);
                if (currentFragment instanceof ArchiveFragment) {
                    ((ArchiveFragment) currentFragment).showFileTypeMenu();
                }
            }
        });

        // Listener per il backstack dei fragment
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                showBottomNavAndFab();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ogni volta che l'app torna in foreground, controlla il timeout
        if (!isAuthenticated || isSessionExpired()) {
            hideUI();
            requestAuthenticationIfNeeded();
        }
    }

    // Controlla se il timeout di sessione è scaduto
    private boolean isSessionExpired() {
        long lastAuth = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getLong(KEY_LAST_AUTH, 0);
        long sessionTimeout = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getLong(KEY_SESSION_TIMEOUT, DEFAULT_SESSION_TIMEOUT_MS);
        return (System.currentTimeMillis() - lastAuth) > sessionTimeout;
    }

    // Richiede autenticazione se necessario
    private void requestAuthenticationIfNeeded() {
        isAuthenticated = false;
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Sblocca SecureNotes")
                .setSubtitle("Autenticati per accedere alle tue note")
                .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK |
                        androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(), "Autenticazione fallita: " + errString, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Toast.makeText(getApplicationContext(), "Autenticazione riuscita!", Toast.LENGTH_SHORT).show();
                // Salva il timestamp dell'autenticazione
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                        .putLong(KEY_LAST_AUTH, System.currentTimeMillis())
                        .apply();
                isAuthenticated = true;
                // Mostra l'interfaccia dopo l'autenticazione riuscita
                showUI();
                // Vai alla sezione Note
                bottomNavigationView.setSelectedItemId(R.id.nav_notes);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Autenticazione non riuscita. Riprova.", Toast.LENGTH_SHORT).show();
            }
        });

        // Verifica se la biometria o le credenziali di sistema sono disponibili
        androidx.biometric.BiometricManager biometricManager = androidx.biometric.BiometricManager.from(this);
        int canAuth = biometricManager.canAuthenticate(
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK |
                        androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL);
        if (canAuth == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS) {
            biometricPrompt.authenticate(promptInfo);
        } else {
            Toast.makeText(this, "Autenticazione biometrica o credenziali di sistema non disponibili", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void navigateTo(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    // Nasconde completamente l'interfaccia utente
    private void hideUI() {
        fragmentContainer.setVisibility(View.GONE);
        bottomNavigationView.setVisibility(View.GONE);
        fab.hide();
    }

    // Mostra l'interfaccia utente dopo l'autenticazione
    private void showUI() {
        fragmentContainer.setVisibility(View.VISIBLE);
        bottomNavigationView.setVisibility(View.VISIBLE);
        if (bottomNavigationView.getSelectedItemId() == R.id.nav_notes) {
            fab.show();
        }
    }

    public void hideBottomNavAndFab() {
        bottomNavigationView.setVisibility(View.GONE);
        fab.hide();
    }

    public void showBottomNavAndFab() {
        bottomNavigationView.setVisibility(View.VISIBLE);
        if (bottomNavigationView.getSelectedItemId() == R.id.nav_notes) {
            fab.show();
        }
    }

    public BottomNavigationView getBottomNavigationView() {
        return bottomNavigationView;
    }
}
