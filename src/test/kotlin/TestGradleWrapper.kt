import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldNotBe
import org.danilopianini.upgradle.UpGradle
import org.danilopianini.upgradle.modules.GradleVersion
import org.danilopianini.upgradle.modules.GradleWrapper
import kotlin.time.ExperimentalTime

@ExperimentalTime
class TestGradleWrapper : StringSpec({
    "Gradle versions should be accessible" {
        GradleWrapper.gradleVersions shouldNotBe emptyList<GradleVersion>()
    }
})
