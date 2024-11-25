import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import com.android.build.gradle.BaseExtension as BaseAndroidExtension

class ComposeConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) = with(target) {
    plugins.run {
      apply("org.jetbrains.kotlin.plugin.compose")
    }

    extensions.configure<BaseAndroidExtension> {
      buildFeatures.apply {
        compose = true
      }
    }

    dependencies {
      add("implementation", libs.findLibrary("compose.runtime").get())
      add("lintChecks", libs.findLibrary("composeLintChecks").get())
    }
  }
}
