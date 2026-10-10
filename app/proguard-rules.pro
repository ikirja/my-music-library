# Keep default Android optimizations. The app has no reflection-based libraries
# beyond kotlinx.serialization, which ships its own consumer rules.
-dontwarn org.jetbrains.annotations.**
