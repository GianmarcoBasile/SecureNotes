package com.gianmarco.securenotes.viewmodel;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.gianmarco.securenotes.ArchivePinManager;
import com.gianmarco.securenotes.BackupWorker;
import com.gianmarco.securenotes.RestoreBackupWorker;

public class SettingsViewModel extends ViewModel {
    private final ArchivePinManager archivePinManager;
    private final MutableLiveData<Boolean> pinEnabledLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> pinVerifiedLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> backupSuccessLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> restoreSuccessLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> themeModeLiveData = new MutableLiveData<>();

    public SettingsViewModel(ArchivePinManager archivePinManager) {
        this.archivePinManager = archivePinManager;
        pinEnabledLiveData.setValue(archivePinManager.isArchivePinEnabled());
    }

    public LiveData<Boolean> isPinEnabled() {
        return pinEnabledLiveData;
    }

    public LiveData<Boolean> isPinVerified() {
        return pinVerifiedLiveData;
    }

    public LiveData<Boolean> getBackupSuccess() {
        return backupSuccessLiveData;
    }

    public LiveData<Boolean> getRestoreSuccess() {
        return restoreSuccessLiveData;
    }

    public LiveData<Integer> getThemeMode() {
        return themeModeLiveData;
    }

    public void enablePin(String pin) {
        archivePinManager.setArchivePin(pin);
        archivePinManager.setArchivePinEnabled(true);
        pinEnabledLiveData.setValue(true);
    }

    public void disablePin() {
        archivePinManager.setArchivePinEnabled(false);
        pinEnabledLiveData.setValue(false);
    }

    public void verifyPin(String pin) {
        boolean verified = archivePinManager.verifyArchivePin(pin);
        pinVerifiedLiveData.setValue(verified);
    }

    public void startBackup(Context context, String password, Uri uri) {
        Data inputData = new Data.Builder()
                .putString("backup_password", password)
                .putString("backup_uri", uri.toString())
                .build();
        OneTimeWorkRequest backupRequest = new OneTimeWorkRequest.Builder(BackupWorker.class)
                .setInputData(inputData)
                .build();
        WorkManager.getInstance(context).enqueue(backupRequest);
        backupSuccessLiveData.setValue(true);
    }

    public void startRestore(Context context, String password, Uri uri) {
        Data inputData = new Data.Builder()
                .putString("restore_password", password)
                .putString("restore_uri", uri.toString())
                .build();
        OneTimeWorkRequest restoreRequest = new OneTimeWorkRequest.Builder(RestoreBackupWorker.class)
                .setInputData(inputData)
                .build();
        WorkManager.getInstance(context).enqueue(restoreRequest);
        restoreSuccessLiveData.setValue(true);
    }

    public void setTheme(Context context, int mode) {
        SharedPreferences prefs = context.getSharedPreferences("secure_notes_prefs", Context.MODE_PRIVATE);
        prefs.edit().putInt("theme_mode", mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
        themeModeLiveData.setValue(mode);
    }
} 