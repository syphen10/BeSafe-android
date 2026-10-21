# kotlinx.serialization: keep generated serializers for @Serializable classes.
-keepclassmembers @kotlinx.serialization.Serializable class com.besafe.app.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Make the shipped app harder to reverse-engineer -----------------------------------------
# Move every renamed class into one flat, meaningless package (no com/besafe/app/safety/... tree).
-repackageclasses ''
-allowaccessmodification
# Hide original file names and drop line numbers from the compiled code.
-renamesourcefileattribute ''
# Strip any logging calls so nothing descriptive is left in the bytecode.
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}
