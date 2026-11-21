# Focus OS Production Proguard / R8 Optimization & Obfuscation Rules

# =============================================================================
# GENERAL & ANNOTATIONS
# =============================================================================
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-dontwarn javax.annotation.**
-dontwarn org.osgi.**

# =============================================================================
# ROOM DATABASE & MODELS
# =============================================================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }
-keep class * extends androidx.room.migration.Migration { *; }
-keep class com.naman.focusos.data.model.** { *; }
-keep class com.naman.focusos.data.db.** { *; }
-keep class com.naman.focusos.data.preferences.** { *; }

# =============================================================================
# APACHE POI & XMLBEANS (FOR EXCEL SPREADSHEETS)
# =============================================================================
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn javax.xml.stream.**
-dontwarn java.awt.**
-dontwarn com.graphbuilder.**
-dontwarn org.apache.commons.codec.**
-dontwarn org.apache.commons.collections4.**
-dontwarn org.apache.commons.io.**
-dontwarn org.apache.commons.math3.**
-dontwarn org.apache.commons.compress.**
-dontwarn org.apache.logging.**
-dontwarn org.apache.log4j.**
-dontwarn org.tukaani.xz.**
-dontwarn com.github.luben.zstd.**
-dontwarn org.brotli.dec.**
-dontwarn org.bouncycastle.**
-dontwarn net.sf.saxon.**
-dontwarn com.zaxxer.**
-dontwarn org.etsi.**
-dontwarn org.w3c.**

-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class org.apache.commons.** { *; }

# =============================================================================
# ANDROID COMPONENTS & WIDGETS
# =============================================================================
-keep class com.naman.focusos.receiver.** { *; }
-keep class com.naman.focusos.widget.** { *; }
-keep class com.naman.focusos.notification.** { *; }

# =============================================================================
# KOTLIN & COROUTINES
# =============================================================================
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }
