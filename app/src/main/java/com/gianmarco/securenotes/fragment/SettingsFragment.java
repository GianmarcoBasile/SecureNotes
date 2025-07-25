package com.gianmarco.securenotes.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.security.GeneralSecurityException;
import java.io.IOException;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;

import com.gianmarco.securenotes.ArchivePinManager;
import com.gianmarco.securenotes.BackupWorker;
import com.gianmarco.securenotes.MainActivity;
import com.gianmarco.securenotes.R;
import com.gianmarco.securenotes.RestoreBackupWorker;
import com.gianmarco.securenotes.viewmodel.SettingsViewModel;

public class SettingsFragment extends Fragment {

    private static final String PREFS_NAME = "secure_notes_prefs";
    private static final String KEY_SESSION_TIMEOUT = "session_timeout";
    private static final long DEFAULT_TIMEOUT = 3 * 60 * 1000; // 3 minuti

    private Spinner spinnerTimeout;
    private Switch switchArchivePin;
    private Button btnChangeArchivePin;
    private Button btnExportBackup;
    private Button btnImportBackup;
    private ArchivePinManager archivePinManager;
    private SharedPreferences prefs;
    private String pendingBackupPassword;
    private String pendingRestorePassword;
    private android.net.Uri pendingRestoreUri;
    private SettingsViewModel viewModel;
    private final ActivityResultLauncher<Intent> backupFilePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                android.net.Uri uri = result.getData().getData();
                if (uri != null && pendingBackupPassword != null) {
                    // Passa tutto al ViewModel!
                    viewModel.startBackup(requireContext(), pendingBackupPassword, uri);
                    android.widget.Toast.makeText(requireContext(), "Backup avviato in background", android.widget.Toast.LENGTH_SHORT).show();
                    pendingBackupPassword = null;
                }
            } else {
                pendingBackupPassword = null;
            }
        }
    );
    private final androidx.activity.result.ActivityResultLauncher<android.content.Intent> restoreFilePickerLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    android.net.Uri uri = result.getData().getData();
                    if (uri != null) {
                        pendingRestoreUri = uri;
                        showRestorePasswordDialog();
                    }
                }
            }
    );
    private Spinner spinnerTheme;
    private boolean isChangingPinSwitch = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);    
        try {
            archivePinManager = new ArchivePinManager(requireContext());
            viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
                public <T extends androidx.lifecycle.ViewModel> T create(Class<T> modelClass) {
                    return (T) new SettingsViewModel(archivePinManager);
                }
            }).get(SettingsViewModel.class);
        } catch (GeneralSecurityException | IOException e) {
            Toast.makeText(requireContext(), "Errore nell'inizializzazione delle impostazioni", Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        spinnerTimeout = view.findViewById(R.id.spinner_timeout);
        switchArchivePin = view.findViewById(R.id.switch_archive_pin);
        btnChangeArchivePin = view.findViewById(R.id.btn_change_archive_pin);
        btnExportBackup = view.findViewById(R.id.btn_export_backup);
        btnImportBackup = view.findViewById(R.id.btn_import_backup);
        spinnerTheme = view.findViewById(R.id.spinner_theme);

        setupTimeoutSpinner();
        setupArchivePinSettings();
        setupBackupButton();
        setupRestoreButton();
        setupThemeSpinner();
    }

    private void setupTimeoutSpinner() {
        String[] timeoutOptions = {
            "1 minuto",
            "3 minuti", 
            "5 minuti",
            "10 minuti"
        };

        long[] timeoutValues = {
            1 * 60 * 1000,  // 1 minuto
            3 * 60 * 1000,  // 3 minuti
            5 * 60 * 1000,  // 5 minuti
            10 * 60 * 1000  // 10 minuti
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            timeoutOptions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTimeout.setAdapter(adapter);

        long currentTimeout = prefs.getLong(KEY_SESSION_TIMEOUT, DEFAULT_TIMEOUT);
        for (int i = 0; i < timeoutValues.length; i++) {
            if (timeoutValues[i] == currentTimeout) {
                spinnerTimeout.setSelection(i);
                break;
            }
        }

        final boolean[] isFirstSelection = {true};
        spinnerTimeout.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                long selectedTimeout = timeoutValues[position];
                prefs.edit().putLong(KEY_SESSION_TIMEOUT, selectedTimeout).apply();
                if (!isFirstSelection[0]) {
                    Toast.makeText(requireContext(), "Timeout sessione aggiornato", Toast.LENGTH_SHORT).show();
                }
                isFirstSelection[0] = false;
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void setupArchivePinSettings() {
        if (archivePinManager == null) return;

        switchArchivePin.setChecked(archivePinManager.isArchivePinEnabled());

        switchArchivePin.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChangingPinSwitch) return;
            if (isChecked) {
                showSetPinDialog();
            }
        });

        switchArchivePin.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                if (switchArchivePin.isChecked()) {
                    showVerifyPinBeforeDisable();
                    return true;
                }
            }
            return false;
        });

        btnChangeArchivePin.setOnClickListener(v -> {
            if (viewModel.isArchivePinEnabled()) {
                showChangePinDialog();
            } else {
                Toast.makeText(requireContext(), "Abilita prima il PIN archivio", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBackupButton() {
        btnExportBackup.setOnClickListener(v -> {
            if (viewModel.isArchivePinEnabled()) {
                AlertDialog.Builder pinDialog = new AlertDialog.Builder(requireContext());
                pinDialog.setTitle("PIN Archivio");
                pinDialog.setMessage("Inserisci il PIN per esportare il backup:");
                final EditText pinInput = new EditText(requireContext());
                pinInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
                pinDialog.setView(pinInput);
                pinDialog.setPositiveButton("Procedi", (dialog, which) -> {
                    String pin = pinInput.getText().toString();
                    if (viewModel.verifyPin(pin)) {
                        showBackupPasswordDialog();
                    } else {
                        Toast.makeText(requireContext(), "PIN errato", Toast.LENGTH_SHORT).show();
                    }
                });
                pinDialog.setNegativeButton("Annulla", null);
                pinDialog.show();
            } else {
                showBackupPasswordDialog();
            }
        });
    }

    private void showBackupPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Password backup");
        builder.setMessage("Inserisci una password per cifrare il backup:");
        final EditText input = new EditText(requireContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);
        builder.setPositiveButton("Procedi", (dialog, which) -> {
            String password = input.getText().toString();
            if (password.length() < 6) {
                Toast.makeText(requireContext(), "Password troppo corta", Toast.LENGTH_SHORT).show();
                return;
            }
            pendingBackupPassword = password;
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/zip");
            intent.putExtra(Intent.EXTRA_TITLE, "SecureNotesBackup.zip");
            backupFilePickerLauncher.launch(intent);
        });
        builder.setNegativeButton("Annulla", null);
        builder.show();
    }

    private void setupRestoreButton() {
        btnImportBackup.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/*");
            restoreFilePickerLauncher.launch(intent);
        });
    }

    private void showSetPinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Imposta PIN Archivio");

        View dialogView = LayoutInflater.from(requireContext()).inflate(android.R.layout.simple_list_item_1, null);
        EditText pinInput = new EditText(requireContext());
        pinInput.setHint("Inserisci PIN (4-8 cifre)");
        pinInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        builder.setView(pinInput);

        builder.setPositiveButton("Imposta", (dialog, which) -> {
            String pin = pinInput.getText().toString();
            if (pin.length() >= 4 && pin.length() <= 8) {
                try {
                    viewModel.enablePin(pin);
                    Toast.makeText(requireContext(), "PIN archivio impostato", Toast.LENGTH_SHORT).show();
                } catch (IllegalArgumentException e) {
                    Toast.makeText(requireContext(), "PIN non valido", Toast.LENGTH_SHORT).show();
                    switchArchivePin.setChecked(false);
                }
            } else {
                Toast.makeText(requireContext(), "PIN deve essere di 4-8 cifre", Toast.LENGTH_SHORT).show();
                switchArchivePin.setChecked(false);
            }
        });

        builder.setNegativeButton("Annulla", (dialog, which) -> {
            switchArchivePin.setChecked(false);
        });

        builder.show();
    }

    private void showChangePinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Cambia PIN Archivio");

        View dialogView = LayoutInflater.from(requireContext()).inflate(android.R.layout.simple_list_item_1, null);
        EditText currentPinInput = new EditText(requireContext());
        currentPinInput.setHint("PIN attuale");
        currentPinInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        
        EditText newPinInput = new EditText(requireContext());
        newPinInput.setHint("Nuovo PIN (4-8 cifre)");
        newPinInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.addView(currentPinInput);
        layout.addView(newPinInput);
        builder.setView(layout);

        builder.setPositiveButton("Cambia", (dialog, which) -> {
            String currentPin = currentPinInput.getText().toString();
            String newPin = newPinInput.getText().toString();

            if (!viewModel.verifyPin(currentPin)) {
                Toast.makeText(requireContext(), "PIN attuale non corretto", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPin.length() >= 4 && newPin.length() <= 8) {
                try {
                    viewModel.enablePin(newPin);
                    Toast.makeText(requireContext(), "PIN archivio cambiato", Toast.LENGTH_SHORT).show();
                } catch (IllegalArgumentException e) {
                    Toast.makeText(requireContext(), "Nuovo PIN non valido", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), "Nuovo PIN deve essere di 4-8 cifre", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Annulla", null);
        builder.show();
    }

    private void showRestorePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Password backup");
        builder.setMessage("Inserisci la password per decifrare il backup:");
        final EditText input = new EditText(requireContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);
        builder.setPositiveButton("Procedi", (dialog, which) -> {
            String password = input.getText().toString();
            if (password.length() < 6) {
                Toast.makeText(requireContext(), "Password troppo corta", Toast.LENGTH_SHORT).show();
                return;
            }
            pendingRestorePassword = password;
            viewModel.startRestore(requireContext(), pendingRestorePassword, pendingRestoreUri);
        });
        builder.setNegativeButton("Annulla", null);
        builder.show();
    }

    private void showVerifyPinBeforeDisable() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Disattiva PIN Archivio");
        builder.setMessage("Inserisci il PIN attuale per disattivare la protezione dell'archivio:");
        final EditText input = new EditText(requireContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        builder.setView(input);
        builder.setPositiveButton("Conferma", (dialog, which) -> {
            String pin = input.getText().toString();
            if (viewModel.verifyPin(pin)) {
                viewModel.disablePin();
                isChangingPinSwitch = true;
                switchArchivePin.setChecked(false);
                isChangingPinSwitch = false;
                Toast.makeText(requireContext(), "PIN archivio disabilitato", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "PIN errato", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Annulla", null);
        builder.show();
    }

    private void setupThemeSpinner() {
        String[] themeOptions = {"Tema di sistema", "Chiaro", "Scuro"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, themeOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTheme.setAdapter(adapter);
        int savedTheme = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        int initialSelection = 0;
        if (savedTheme == AppCompatDelegate.MODE_NIGHT_NO) initialSelection = 1;
        else if (savedTheme == AppCompatDelegate.MODE_NIGHT_YES) initialSelection = 2;
        spinnerTheme.setSelection(initialSelection);
        final int finalInitialSelection = initialSelection;
        spinnerTheme.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            boolean first = true;
            int lastSelection = finalInitialSelection;
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (first) { first = false; return; }
                if (position == lastSelection) return;
                int mode;
                switch (position) {
                    case 1: mode = AppCompatDelegate.MODE_NIGHT_NO; break;
                    case 2: mode = AppCompatDelegate.MODE_NIGHT_YES; break;
                    default: mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                }
                viewModel.setTheme(requireContext(), mode);
                prefs.edit().putBoolean("skip_auth_on_next_start", true).apply();
                requireActivity().recreate();
                
                int selectedItemId = ((MainActivity) requireActivity()).getBottomNavigationView().getSelectedItemId();
                prefs.edit().putInt("last_section", selectedItemId).apply();
                lastSelection = position;
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }
} 