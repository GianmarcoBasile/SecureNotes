package com.gianmarco.securenotes;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
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

import java.security.GeneralSecurityException;
import java.io.IOException;

public class SettingsFragment extends Fragment {

    private static final String PREFS_NAME = "secure_notes_prefs";
    private static final String KEY_SESSION_TIMEOUT = "session_timeout";
    private static final long DEFAULT_TIMEOUT = 3 * 60 * 1000; // 3 minuti

    private Spinner spinnerTimeout;
    private Switch switchArchivePin;
    private Button btnChangeArchivePin;
    private Button btnExportBackup;
    private ArchivePinManager archivePinManager;
    private SharedPreferences prefs;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        try {
            archivePinManager = new ArchivePinManager(requireContext());
        } catch (GeneralSecurityException | IOException e) {
            Toast.makeText(requireContext(), "Errore nell'inizializzazione delle impostazioni", Toast.LENGTH_SHORT).show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        spinnerTimeout = view.findViewById(R.id.spinner_timeout);
        switchArchivePin = view.findViewById(R.id.switch_archive_pin);
        btnChangeArchivePin = view.findViewById(R.id.btn_change_archive_pin);
        btnExportBackup = view.findViewById(R.id.btn_export_backup);

        setupTimeoutSpinner();
        setupArchivePinSettings();
        setupBackupButton();
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

        // Imposta il valore corrente
        long currentTimeout = prefs.getLong(KEY_SESSION_TIMEOUT, DEFAULT_TIMEOUT);
        for (int i = 0; i < timeoutValues.length; i++) {
            if (timeoutValues[i] == currentTimeout) {
                spinnerTimeout.setSelection(i);
                break;
            }
        }

        spinnerTimeout.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                long selectedTimeout = timeoutValues[position];
                prefs.edit().putLong(KEY_SESSION_TIMEOUT, selectedTimeout).apply();
                Toast.makeText(requireContext(), "Timeout sessione aggiornato", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void setupArchivePinSettings() {
        if (archivePinManager == null) return;

        // Imposta lo stato corrente dello switch
        switchArchivePin.setChecked(archivePinManager.isArchivePinEnabled());

        switchArchivePin.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Abilita PIN - richiedi di impostarlo
                showSetPinDialog();
            } else {
                // Disabilita PIN
                archivePinManager.setArchivePinEnabled(false);
                Toast.makeText(requireContext(), "PIN archivio disabilitato", Toast.LENGTH_SHORT).show();
            }
        });

        btnChangeArchivePin.setOnClickListener(v -> {
            if (archivePinManager.isArchivePinEnabled()) {
                showChangePinDialog();
            } else {
                Toast.makeText(requireContext(), "Abilita prima il PIN archivio", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBackupButton() {
        btnExportBackup.setOnClickListener(v -> {
            // TODO: Implementare esportazione backup criptato
            Toast.makeText(requireContext(), "Funzionalità backup in sviluppo", Toast.LENGTH_SHORT).show();
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
                    archivePinManager.setArchivePin(pin);
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

            if (!archivePinManager.verifyArchivePin(currentPin)) {
                Toast.makeText(requireContext(), "PIN attuale non corretto", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPin.length() >= 4 && newPin.length() <= 8) {
                try {
                    archivePinManager.setArchivePin(newPin);
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
} 