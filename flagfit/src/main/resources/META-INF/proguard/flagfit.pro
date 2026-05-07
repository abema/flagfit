# Keep flagfit annotation classes individually (avoid library-package wildcard
# per the Android library-optimization guidance). Lookups in Flagfit.kt use
# class literals which R8 rewrites in lock-step, so allowobfuscation is safe.
-keep,allowobfuscation class tv.abema.flagfit.annotation.BooleanFlag
-keep,allowobfuscation class tv.abema.flagfit.annotation.VariationFlag
-keep,allowobfuscation class tv.abema.flagfit.annotation.BooleanEnv
-keep,allowobfuscation class tv.abema.flagfit.annotation.DebugWith
-keep,allowobfuscation class tv.abema.flagfit.annotation.ReleaseWith
-keep,allowobfuscation class tv.abema.flagfit.annotation.DefaultWith
-keep,allowobfuscation class tv.abema.flagfit.SuspendReturnType

# Annotation values must survive at runtime so Flagfit.create() can read them
# via reflection. Only RuntimeVisibleAnnotations belongs in consumer rules per
# the Android library-optimization guidance — Flagfit does not read annotation
# defaults or generic signatures, so AnnotationDefault and Signature are not
# needed here.
-keepattributes RuntimeVisibleAnnotations

# Flagfit.create() builds a JDK Proxy for any interface whose methods are
# annotated with @BooleanFlag or @VariationFlag, then iterates declaredMethods.
# R8 fullMode removes such interfaces because it sees no concrete
# implementation, so explicitly keep them.
#
# NOTE: do NOT add ,allowshrinking here. Although Retrofit ships its
# equivalent rule with allowshrinking, empirically adding it causes R8
# fullMode to tree-shake the annotated methods on user flag-service
# interfaces (because the only dynamic caller is Proxy.invoke via reflection,
# which R8 does not trace), which re-introduces the issue this rule fixes.
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

# FlagSource subclasses referenced only via @DebugWith / @ReleaseWith /
# @DefaultWith (KClass<out FlagSource> annotation values) are otherwise
# unreachable. allowobfuscation is safe (R8 rewrites class literals); do NOT
# add allowshrinking — under R8 fullMode KClass annotation values are not
# always traced as live references and dropping them would re-introduce #39.
-keep,allowobfuscation class * extends tv.abema.flagfit.FlagSource
