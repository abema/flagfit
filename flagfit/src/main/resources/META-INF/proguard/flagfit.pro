# Keep flagfit annotation classes themselves.
-keep class tv.abema.flagfit.annotation.**
-keep class tv.abema.flagfit.SuspendReturnType

# Annotations must survive with their runtime values so Flagfit.create() can read
# them via reflection at runtime.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,Signature

# Flagfit.create() builds a JDK Proxy for any interface whose methods are annotated
# with @BooleanFlag or @VariationFlag, then iterates declaredMethods. R8 fullMode
# removes such interfaces because it sees no concrete implementation, so explicitly
# keep them. (Same pattern Retrofit uses for @retrofit2.http.* methods.)
-if interface * { @tv.abema.flagfit.annotation.BooleanFlag <methods>; }
-keep,allowobfuscation interface <1>

-if interface * { @tv.abema.flagfit.annotation.VariationFlag <methods>; }
-keep,allowobfuscation interface <1>

# Also keep subinterfaces of the above, because callers may proxy a child
# interface (e.g. `interface Child : Parent`) whose annotated methods live on
# the parent. R8 fullMode would otherwise be free to strip / merge `Child`.
-if interface * { @tv.abema.flagfit.annotation.BooleanFlag <methods>; }
-keep,allowobfuscation interface * extends <1>

-if interface * { @tv.abema.flagfit.annotation.VariationFlag <methods>; }
-keep,allowobfuscation interface * extends <1>

# Keep the annotated methods (and their annotations) on those interfaces so
# Method.getAnnotations() returns the real values.
-keepclassmembers,allowobfuscation interface * {
    @tv.abema.flagfit.annotation.BooleanFlag <methods>;
    @tv.abema.flagfit.annotation.VariationFlag <methods>;
    @tv.abema.flagfit.annotation.BooleanEnv <methods>;
    @tv.abema.flagfit.annotation.DebugWith <methods>;
    @tv.abema.flagfit.annotation.ReleaseWith <methods>;
    @tv.abema.flagfit.annotation.DefaultWith <methods>;
}

# FlagSource subclasses referenced only via @DebugWith / @ReleaseWith / @DefaultWith
# (KClass<out FlagSource> annotation values) are otherwise unreachable.
-keep class * extends tv.abema.flagfit.FlagSource
