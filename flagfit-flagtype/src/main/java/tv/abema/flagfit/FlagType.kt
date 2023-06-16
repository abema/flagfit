package tv.abema.flagfit

import kotlin.reflect.KClass

class FlagType {
  /**
   * > Release Toggles allow incomplete and un-tested codepaths to be shipped to production as latent code which may never be turned on.
   *
   * [Release Toggles](https://martinfowler.com/articles/feature-toggles.html#ReleaseToggles)
   */
  annotation class WorkInProgress(
    val owner: String,
    val description: String,
    val expiryDate: String,
  )

  /**
   * > Experiment Toggles are used to perform multivariate or A/B testing.
   *
   * [Experiment Toggles](https://martinfowler.com/articles/feature-toggles.html#ExperimentToggles)
   */
  annotation class Experiment(
    val owner: String,
    val description: String,
    val expiryDate: String,
  )

  /**
   * > These flags are used to control operational aspects of our system's behavior.
   *
   * [Ops Toggles](https://martinfowler.com/articles/feature-toggles.html#OpsToggles)
   */
  annotation class Ops(
    val owner: String,
    val description: String,
    val expiryDate: String = "",
  )

  /**
   * > These flags are used to change the features or product experience that certain users receive.
   *
   * [Permissioning Toggles](https://martinfowler.com/articles/feature-toggles.html#PermissioningToggles)
   */
  annotation class Permission(
    val owner: String,
    val description: String,
    val expiryDate: String = "",
  )

  class WorkInProgressAnnotationAdapter : AnnotationAdapter<WorkInProgress> {
    override fun canHandle(
      annotation: WorkInProgress,
      env: Map<String, Any>,
    ): Boolean {
      return true
    }

    override fun flagSourceClass(annotation: WorkInProgress): KClass<JustFlagSource.False> {
      return JustFlagSource.False::class
    }

    override fun annotationClass(): KClass<WorkInProgress> {
      return WorkInProgress::class
    }
  }

  class OpsAnnotationAdapter : AnnotationAdapter<Ops> {
    override fun canHandle(
      annotation: Ops,
      env: Map<String, Any>,
    ): Boolean {
      return true
    }

    override fun flagSourceClass(annotation: Ops): KClass<OpsFlagSource> {
      return OpsFlagSource::class
    }

    override fun annotationClass(): KClass<Ops> {
      return Ops::class
    }
  }

  class ExperimentAnnotationAdapter : AnnotationAdapter<Experiment> {
    override fun canHandle(
      annotation: Experiment,
      env: Map<String, Any>,
    ): Boolean {
      return true
    }

    override fun flagSourceClass(annotation: Experiment): KClass<ExperimentFlagSource> {
      return ExperimentFlagSource::class
    }

    override fun annotationClass(): KClass<Experiment> {
      return Experiment::class
    }
  }

  class PermissionAnnotationAdapter : AnnotationAdapter<Permission> {
    override fun canHandle(
      annotation: Permission,
      env: Map<String, Any>,
    ): Boolean {
      return true
    }

    override fun flagSourceClass(annotation: Permission): KClass<PermissionFlagSource> {
      return PermissionFlagSource::class
    }

    override fun annotationClass(): KClass<Permission> {
      return Permission::class
    }
  }

  companion object {
    fun annotationAdapters() = listOf(
      WorkInProgressAnnotationAdapter(),
      ExperimentAnnotationAdapter(),
      OpsAnnotationAdapter(),
      PermissionAnnotationAdapter()
    )
  }
}
