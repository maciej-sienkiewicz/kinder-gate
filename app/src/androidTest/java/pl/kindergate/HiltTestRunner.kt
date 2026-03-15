package pl.kindergate

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Custom test runner that replaces the Application with HiltTestApplication.
 * Required for Hilt dependency injection in instrumented tests.
 *
 * Configure in app/build.gradle.kts:
 *   testInstrumentationRunner = "pl.kindergate.HiltTestRunner"
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, name: String?, context: Context?): Application =
        super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
