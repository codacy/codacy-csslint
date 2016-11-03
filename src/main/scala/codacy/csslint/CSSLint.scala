package codacy.csslint

import codacy.docker.api._
import codacy.dockerApi.utils.CommandRunner

import scala.util.{Failure, Success, Try}

object CSSLint extends Tool {

  lazy val CssLintMatch = """(.*): line ([0-9]*),.*, ([a-zA-Z]+) - (.*) \((.*)\)""".r

  override def apply(source: Source.Directory, configuration: Option[List[Pattern.Definition]], files: Option[Set[Source.File]])
                    (implicit specification: Tool.Specification): Try[List[Result]] = {

    val sourceDir = better.files.File(source.path)

    val filesToLint = files.map(_.map(_.path)).getOrElse(Set(source.path))
    val command = List("csslint", "--format=compact") /*++ nativeConfigParams */++ filesToLint

    if(configuration.isDefined) (better.files.File(source.path) / ".csslintrc").delete(swallowIOExceptions = true)

    CommandRunner.exec(command, dir = Some(sourceDir.toJava))
      .fold(Failure.apply, res => Success(parseResult(res.stdout, configuration)))
  }

  def parseResult(lines: List[String], fullConfig: Option[List[Pattern.Definition]]): List[Result] = {
    lines.flatMap {
      case CssLintMatch(file, line, level, message, patternId)
        if fullConfig.forall(_.exists(_.patternId.value == patternId)) =>
        Try(line.toInt).map { case rLine =>
          Result.Issue(Source.File(file), Result.Message(message), Pattern.Id(patternId), Source.Line(rLine))
        }.toOption
      case _ => List.empty
    }
  }
}
