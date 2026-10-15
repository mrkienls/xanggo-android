# Quy tắc R8 cho bản release XăngGo.

# Giữ số dòng trong stack trace để đọc crash report trên Play Console
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# Giữ model dữ liệu của XăngGo
-keep class com.example.xanggo.data.model.** { *; }

# Giữ WorkManager & Room (dùng bởi Google Mobile Ads và androidx.startup)
-keep class androidx.work.** { *; }
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class androidx.work.impl.WorkDatabase_Impl {
    public <init>();
}
-keep class androidx.startup.** { *; }
-dontwarn androidx.work.**
-dontwarn androidx.room.**

# Firebase Firestore & Google Play Services
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-dontwarn io.grpc.**

# Google Mobile Ads
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }
-dontwarn com.google.android.gms.ads.**
