# FlagType outer class and all inner classes (FlagType.WorkInProgress,
# .Experiment, .Ops, .Permission and the four *AnnotationAdapter inner
# classes) must survive because Flagfit.create() reflects on the annotations
# and FlagType.annotationAdapters() instantiates the adapters.
# Class literal references in user code are rewritten by R8 in lock-step,
# so allowobfuscation is safe.
-keep,allowobfuscation class tv.abema.flagfit.FlagType
-keep,allowobfuscation class tv.abema.flagfit.FlagType$* { *; }
