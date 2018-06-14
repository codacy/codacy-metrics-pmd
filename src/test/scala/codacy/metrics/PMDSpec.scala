package codacy.metrics

import codacy.docker.api.Source
import codacy.docker.api.metrics.{FileMetrics, LineComplexity}
import com.codacy.api.dtos.Languages
import org.specs2.mutable.Specification

class PMDSpec extends Specification {

  val testFileExpectedMetrics = FileMetrics(
    filename = "codacy/metrics/test.java",
    complexity = Some(1),
    lineComplexities = Set(LineComplexity(43, 1), LineComplexity(49, 1), LineComplexity(53, 1), LineComplexity(57, 1)))

  val likeFileExpectedMetrics = FileMetrics(
    filename = "codacy/metrics/Like.java",
    complexity = Some(13),
    lineComplexities = Set(
      LineComplexity(57, 1),
      LineComplexity(73, 1),
      LineComplexity(27, 13),
      LineComplexity(8, 1),
      LineComplexity(53, 1),
      LineComplexity(69, 1),
      LineComplexity(65, 1),
      LineComplexity(61, 1),
      LineComplexity(16, 7),
      LineComplexity(78, 1)))

  val expectedFileMetrics = List(testFileExpectedMetrics, likeFileExpectedMetrics)

  val targetDir = "src/test/resources"

  "PMD" should {

    "get metrics" in {

      "all Java files within a directory" in {

        "without specifying the language" in {
          val fileMetricsMap =
            PMD(source = Source.Directory(targetDir), language = None, files = None, options = Map.empty)

          fileMetricsMap should beSuccessfulTry(containTheSameElementsAs(expectedFileMetrics))
        }

        "specifying the Java language" in {
          val fileMetricsMap =
            PMD(
              source = Source.Directory(targetDir),
              language = Some(Languages.Java),
              files = None,
              options = Map.empty)

          fileMetricsMap should beSuccessfulTry(containTheSameElementsAs(expectedFileMetrics))
        }
      }

      "specific files" in {
        val fileMetricsMap = PMD(
          source = Source.Directory(targetDir),
          language = None,
          files = Some(Set(Source.File(likeFileExpectedMetrics.filename))),
          options = Map.empty)

        fileMetricsMap should beSuccessfulTry(containTheSameElementsAs(List(likeFileExpectedMetrics)))
      }
    }

    "fail if the language isn't Java" in {
      val rubyLang = Languages.Ruby
      val fileMetricsMap = PMD(
        source = Source.Directory(targetDir),
        language = Some(rubyLang),
        files = Some(Set(Source.File(likeFileExpectedMetrics.filename))),
        options = Map.empty)

      fileMetricsMap should beFailedTry.withThrowable[Exception](
        s"PMD metrics only supports Java. Provided language: $rubyLang")
    }
  }
}
