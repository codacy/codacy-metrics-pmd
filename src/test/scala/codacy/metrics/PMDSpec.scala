package codacy.metrics

import codacy.docker.api.Source
import codacy.docker.api.metrics.{FileMetrics, LineComplexity}
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

      "all files within a directory" in {
        val fileMetricsMap =
          PMD(source = Source.Directory(targetDir), language = None, files = None, options = Map.empty)

        fileMetricsMap should beSuccessfulTry(containTheSameElementsAs(expectedFileMetrics))
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
  }
}
