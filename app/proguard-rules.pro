# Room 的實作類別由 KSP 產生、以反射載入；Room 自帶的 consumer rules 已涵蓋，
# 這裡只保留由系統以反射建立的元件。
-keep class com.routina.glance.GlanceApp
-keep class com.routina.glance.MainActivity
-keep class com.routina.glance.capture.GlanceListener
