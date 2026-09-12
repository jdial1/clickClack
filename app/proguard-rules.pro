# Compose and Firebase ship their own consumer rules, so this file stays small
# on purpose. Only what R8 cannot work out for itself belongs here.

# Model classes are read back out of Firestore snapshots by field name. Nothing
# references those names from Kotlin, so R8 is right to think they are unused
# and wrong to remove them.
-keep class com.ampersand.model.** { *; }

# Enum valueOf is used to turn stored strings back into Reaction, BrickHeight
# and friends, and it is reflective.
-keepclassmembers enum com.ampersand.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
