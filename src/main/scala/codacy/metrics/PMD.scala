package codacy.metrics
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
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
import scala.util.{Failure, Success, Try}
import scala.xml.{Elem, XML}

object PMD extends MetricsTool {

  override def apply(source: Source.Directory,
                     language: Option[Language],
                     files: Option[Set[Source.File]],
                     options: Map[MetricsConfiguration.Key, MetricsConfiguration.Value]): Try[List[FileMetrics]] = {
    for {
      _ <- language
        .find(_ != Languages.Java)
        .fold[Try[Language]](Success(Languages.Java))(lang =>
          Failure(new Exception(s"PMD metrics only supports Java. Provided language: $lang")))
      pmdConfig <- buildConfig(source.path, files)
      complexity <- calculateComplexity(pmdConfig, source.path)
    } yield {
      complexity
    }
  }

  private def calculateComplexity(pmdConfig: PMDConfiguration, directory: String): Try[List[FileMetrics]] = Try {
    val ruleSetFactory = RulesetsFactoryUtils.getRulesetFactory(pmdConfig, new ResourceLoader())

    val languages = new java.util.HashSet[pmd.lang.Language]
    languages.add(new JavaLanguageModule())

    val applicableFiles = pmd.PMD.getApplicableFiles(pmdConfig, languages)

    val codacyRenderer = new CodacyInMemoryRenderer()
    val renderers = Collections.singletonList(codacyRenderer: Renderer)

    pmd.PMD.processFiles(pmdConfig, ruleSetFactory, applicableFiles, new RuleContext, renderers)

    val rulesViolations = codacyRenderer.getRulesViolations.asScala
    createFileMetrics(rulesViolations, directory)
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

    val baos = new ByteArrayOutputStream()

    tmpConfigfile(xmlConfiguration) match {
      case Success(p) =>
        pmdConfig.setRuleSets(p.toString)
        Success(pmdConfig)
      case Failure(e) =>
        val errString = new String(baos.toByteArray, StandardCharsets.UTF_8)
        val msg =
          s"""|Failed to execute duplication: ${e.getMessage}
              |std:
              |$errString
         """.stripMargin

        Failure(new Exception(msg, e))
    }

  }

  private def tmpConfigfile(content: Elem): Try[Path] = {
    Try {
      val tmpFile = Files.createTempFile("ruleset", ".xml")
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
