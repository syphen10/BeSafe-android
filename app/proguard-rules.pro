# kotlinx.serialization: keep generated serializers for @Serializable classes.
-keepclassmembers @kotlinx.serialization.Serializable class com.besafe.app.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
