package codacy.metrics
import java.nio.file.{Files, Path, Paths}
import java.util.Collections

import better.files._
import codacy.docker.api.metrics.{FileMetrics, LineComplexity, MetricsTool}
import codacy.docker.api.{MetricsConfiguration, Source}
import com.codacy.api.dtos.{Language, Languages}
import com.codacy.docker.api.utils.FileHelper
import net.sourceforge.pmd
import net.sourceforge.pmd._
import net.sourceforge.pmd.lang.java.JavaLanguageModule
import net.sourceforge.pmd.renderers.Renderer
import net.sourceforge.pmd.util.ResourceLoader

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.{Failure, Try}
import scala.xml.{Elem, XML}

object PMD extends MetricsTool {

  override def apply(source: Source.Directory,
                     language: Option[Language],
                     files: Option[Set[Source.File]],
                     options: Map[MetricsConfiguration.Key, MetricsConfiguration.Value]): Try[List[FileMetrics]] = {

    language match {
      case Some(lang) if lang != Languages.Java =>
        Failure(new Exception(s"PMD metrics only supports Java. Provided language: $lang"))
      case _ =>
        calculateComplexity(source.path, files)
    }
  }

  private def calculateComplexity(directory: String, files: Option[Set[Source.File]]): Try[List[FileMetrics]] = {

    val pmdConfig = buildConfig(directory, files)

    val ruleSetFactory = RulesetsFactoryUtils.getRulesetFactory(pmdConfig, new ResourceLoader())

    val languages = new java.util.HashSet[net.sourceforge.pmd.lang.Language]
    languages.add(new JavaLanguageModule())

    val applicableFiles = pmd.PMD.getApplicableFiles(pmdConfig, languages)

    Try {
      val codacyRenderer = new CodacyInMemoryRenderer()
      val renderer: Renderer = codacyRenderer
      val renderers = Collections.singletonList(renderer)

      pmd.PMD.processFiles(pmdConfig, ruleSetFactory, applicableFiles, new RuleContext, renderers)

      val rulesViolations = codacyRenderer.getRulesViolations.asScala
      createFileMetrics(rulesViolations, directory)
    }
  }

  private def createFileMetrics(rulesViolations: mutable.Buffer[RuleViolation], srcPath: String): List[FileMetrics] = {
    rulesViolations
      .groupBy(_.getFilename)
      .map {
        case (fileName, violations) =>
          val lineComplexities: Set[LineComplexity] = violations.flatMap { violation =>
            parseComplexityMessage(violation.getDescription).map(value => LineComplexity(violation.getBeginLine, value))
          }(collection.breakOut)
          val complexity = Some((lineComplexities.map(_.value) + 0).max)
          FileMetrics(
            relativizeToolOutputPath(fileName, srcPath),
            complexity = complexity,
            lineComplexities = lineComplexities)
      }(collection.breakOut)
  }

  @SuppressWarnings(Array("NullParameter"))
  private def buildConfig(directory: String, files: Option[Set[Source.File]]) = {
    val pmdConfig = new PMDConfiguration()

    //this is to avoid a warning printed to the console
    pmdConfig.setIgnoreIncrementalAnalysis(true)

    files.fold[Unit] {
      pmdConfig.setInputPaths(directory)
    } { files =>
      val filesStr = files.map(f => directory / f.path).mkString(",")
      pmdConfig.setInputPaths(filesStr)
    }

    val xmlConfiguration =
      <ruleset name="Codacy Ruleset"
               xmlns="http://pmd.sourceforge.net/ruleset/2.0.0"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://pmd.sourceforge.net/ruleset/2.0.0 http://pmd.sourceforge.net/ruleset_2_0_0.xsd">
        <description>Codacy UI Ruleset</description>
        <rule ref="category/java/design.xml/CyclomaticComplexity">
          <properties>
            <property name="methodReportLevel" value="1"/>
          </properties>
        </rule>
      </ruleset>
    val config = fileForConfig(xmlConfiguration)

    config.map(p => pmdConfig.setRuleSets(p.toString))
    pmdConfig
  }

  private def fileForConfig(config: Elem) = tmpfile(config)

  private def tmpfile(content: Elem, prefix: String = "ruleset", suffix: String = ".xml"): Try[Path] = {
    Try {
      val tmpFile = Files.createTempFile(prefix, suffix)
      XML.save(tmpFile.toAbsolutePath.toString, content, "UTF-8", xmlDecl = true, null)
      tmpFile
    }
  }

  private def parseComplexityMessage(message: String): Option[Int] = {
    val complexityRegex = """The .*? has a cyclomatic complexity of (\d+).""".r
    message match {
      case complexityRegex(n) => Try(n.toInt).toOption
      case _                  => None
    }
  }

  private def relativizeToolOutputPath(filePath: String, srcPath: String): String = {
    FileHelper.stripPath(filePath, Paths.get(srcPath).toAbsolutePath.toString)
  }

}
