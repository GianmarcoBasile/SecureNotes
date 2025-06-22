package com.gianmarco.securenotes;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

import com.gianmarco.securenotes.fragment.ArchiveFragment;
import com.gianmarco.securenotes.fragment.DashboardFragment;
import com.gianmarco.securenotes.fragment.SettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.fragment.app.Fragment;

import android.os.Build;
import android.Manifest;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import com.scottyab.rootbeer.RootBeer;

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
        int themeMode = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getInt("theme_mode", androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(themeMode);

        boolean skipAuth = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean("skip_auth_on_next_start", false);

        if (skipAuth) {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putBoolean("skip_auth_on_next_start", false).apply();
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            bottomNavigationView = findViewById(R.id.bottom_navigation);
            fab = findViewById(R.id.fab_add_note);
            fragmentContainer = findViewById(R.id.fragment_container);
            setupBottomNavigation();
            showUI();
            int lastSection = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getInt("last_section", R.id.nav_notes);
            bottomNavigationView.setSelectedItemId(lastSection);
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().remove("last_section").apply();
            isAuthenticated = true;
            return;
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fab = findViewById(R.id.fab_add_note);
        fragmentContainer = findViewById(R.id.fragment_container);
        
        hideUI();
        
        RootBeer rootBeer = new RootBeer(this);
        if (rootBeer.isRooted()) {
            Toast.makeText(this, "Dispositivo rootato! L'app verrà chiusa per sicurezza.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        requestAuthenticationIfNeeded();

        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isAuthenticated || isSessionExpired()) {
            hideUI();
            requestAuthenticationIfNeeded();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1002) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                android.widget.Toast.makeText(this, "Permesso notifiche negato: le notifiche non saranno mostrate", android.widget.Toast.LENGTH_LONG).show();
            }
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
                // Richiedi permesso notifiche solo dopo autenticazione
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1002);
                    }
                }
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
            Toast.makeText(this, "Autenticazione biometrica o credenziali di sistema necessarie per l'applicazione", Toast.LENGTH_LONG).show();
            finish();
        }

    }

    private void navigateTo(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void hideUI() {
        fragmentContainer.setVisibility(View.GONE);
        bottomNavigationView.setVisibility(View.GONE);
        fab.hide();
    }

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

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_notes) {
                fab.show();
                fab.setOnClickListener(v -> {
                    getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, com.gianmarco.securenotes.fragment.EditorFragment.newInstance(-1))
                        .addToBackStack(null)
                        .commit();
                });
                navigateTo(new com.gianmarco.securenotes.fragment.DashboardFragment());
                return true;
            } else if (itemId == R.id.nav_archive) {
                fab.show();
                fab.setOnClickListener(v -> {
                    androidx.fragment.app.Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    if (current instanceof com.gianmarco.securenotes.fragment.ArchiveFragment) {
                        ((com.gianmarco.securenotes.fragment.ArchiveFragment) current).showFileTypeMenu();
                    }
                });
                navigateTo(new com.gianmarco.securenotes.fragment.ArchiveFragment());
                return true;
            } else if (itemId == R.id.nav_settings) {
                fab.hide();
                navigateTo(new com.gianmarco.securenotes.fragment.SettingsFragment());
                return true;
            }
            return false;
        });
    }
}
