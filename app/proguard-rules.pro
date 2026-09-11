# 保留 kotlinx.serialization 生成的序列化器与数据类字段（配置解析依赖）
-keepclassmembers class com.example.cmdprompter.data.model.** {
    <fields>;
}
-keepclassmembers class com.example.cmdprompter.data.model.**$* {
    <fields>;
}
-keepclassmembers class com.example.cmdprompter.data.model.** {
    *** Companion;
}
-keep,includedescriptorclasses class com.example.cmdprompter.data.model.**$$serializer { *; }

# 避免 assets 中的配置被资源压缩移除
-keepdirectories app/src/main/assets/**
