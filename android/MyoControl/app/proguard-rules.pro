# Add project-specific ProGuard rules here.
# Keep BLE manager and domain models from being stripped
-keep class com.ian.myocontrol.core.ble.** { *; }
-keep class com.ian.myocontrol.domain.model.** { *; }
