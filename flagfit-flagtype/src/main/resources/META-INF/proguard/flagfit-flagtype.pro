# FlagType outer class and all inner classes (FlagType.WorkInProgress, .Experiment,
# .Ops, .Permission and the four *AnnotationAdapter inner classes) must survive
# because Flagfit.create() reflects on the annotations and FlagType.annotationAdapters()
# instantiates the adapters by name.
-keep class tv.abema.flagfit.FlagType
-keep class tv.abema.flagfit.FlagType$* { *; }
