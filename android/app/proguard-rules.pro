# kotlinx.serialization يشحن قواعده؛ نحافظ على نماذج core المسلسلة احتياطاً
-keep,includedescriptorclasses class app.sanad.core.**$$serializer { *; }
-keepclassmembers class app.sanad.core.** { *** Companion; }
-keepclasseswithmembers class app.sanad.core.** { kotlinx.serialization.KSerializer serializer(...); }
