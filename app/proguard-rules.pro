# Mantieni tutte le classi di AndroidX (necessario per compatibilità librerie)
-keep class androidx.** { *; }

# Mantieni classi usate dalla Biometric API
-keep class androidx.biometric.** { *; }

# Mantieni entità Room (aggiungi qui tutte le tue entity)
-keep class com.gianmarco.securenotes.note.Note { *; }
-keep class com.gianmarco.securenotes.file.SecureFile { *; }

# Mantieni i metodi annotati Room (costruttori, DAO, ecc)
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Mantieni classi per EncryptedSharedPreferences, EncryptedFile, ecc
-keep class androidx.security.crypto.** { *; }

# Mantieni l'entry point dell'app (decommenta se hai una classe Application custom)
# -keep class com.gianmarco.securenotes.SecureNotesApplication { *; }

# Per evitare errori con riflessione (es. Gson, Room)
-keepnames class * {
    @com.google.gson.annotations.SerializedName <fields>;
}