package net.litola

import sbt.PlayExceptions.AssetCompilationException
import java.io.File
import scala.sys.process._
import sbt.IO
import io.Source._

object SassCompiler {
  def compile(sassFile: File, options: Seq[String]): (String, Option[String], Seq[File]) = {
    try {
      val parentPath = sassFile.getParentFile.getAbsolutePath
      val cssOutput = runCompiler(
        Seq("sass", "-I", parentPath) ++ options ++ Seq(sassFile.getAbsolutePath)
        )
      val compressedCssOutput = runCompiler(
        Seq("sass", "-t", "compressed", "-I", parentPath) ++ options ++ Seq(sassFile.getAbsolutePath)
        )

      (cssOutput, Some(compressedCssOutput), Seq(sassFile))
    } catch {
      case e: SassCompilationException => {
        throw AssetCompilationException(e.file.orElse(Some(sassFile)), "Sass compiler: " + e.message, e.line, e.column)
      }
    }
  }

  private def runCompiler(command: ProcessBuilder): String = {
    val err = new StringBuilder
    val out = new StringBuilder

    val capturer = ProcessLogger(
      (output: String) => out.append(output + "\n"),
      (error: String) => err.append(error + "\n"))

    val process = command.run(capturer)
    if (process.exitValue == 0)
      out.mkString
    else
      throw new SassCompilationException(err.toString)


  }

  private val LocationLine = """\s*on line (\d+) of (.*)""".r

  private class SassCompilationException(stderr: String) extends RuntimeException {

    val (file: Option[File], line: Int, column: Int, message: String) = parseError(stderr)

    private def parseError(error: String): (Option[File], Int, Int, String) = {
      var line = 0
      var seen = 0
      var column = 0
      var file : Option[File] = None
      var message = "Unknown error, try running sass directly"
      for (errline: String <- augmentString(error).lines) {
        errline match {
          case LocationLine(l, f) => { line = l.toInt; file = Some(new File(f)); }
          case other if (seen == 0) => { message = other; seen += 1 }
          case other =>
        }
      }
      (file, line, column, message)
    }
  }
}
