import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import org.danilopianini.upgradle.modules.GradleVersion

fun version(origin: String) = GradleVersion.fromGithubRelease(origin)

class TestGradleVersion : StringSpec({
    "RC versions should be younger than stable" {
        val rcversion = "6.0 RC1"
        version("6.0")!! shouldBeGreaterThan version(rcversion)!!
        version("6.0.1")!! shouldBeGreaterThan version(rcversion)!!
        version(rcversion)!! shouldBeLessThan version("6.0.1")!!
        version("5.99")!! shouldBeLessThan version(rcversion)!!
        println(version("6.0.1")?.compareTo(version(rcversion)!!))
    }

})