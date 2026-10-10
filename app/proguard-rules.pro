# Quy tắc R8 cho bản release XăngGo.
# Firebase, AdMob, Compose đã kèm consumer rules riêng.
# App tự parse Firestore/JSON thủ công (không dùng reflection) nên không cần keep model.

# Giữ số dòng trong stack trace để đọc crash report trên Play Console
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
