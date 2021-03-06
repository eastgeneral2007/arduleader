package com.geeksville.shell

import java.io._
import scala.tools.nsc.GenericRunnerSettings
import scala.tools.jline.Terminal
import scala.tools.jline.TerminalFactory

class ScalaShell(val in: InputStream = System.in, val out: OutputStream = System.out) {

  def name = "shell"

  def initCmds = Seq[String]()

  private var bindings: Map[String, (java.lang.Class[_], AnyRef)] = Map()

  private val pw = new PrintWriter(out)

  protected def bind[A <: AnyRef](name: String, value: A)(implicit m: Manifest[A]): Unit =
    bindings += ((name, (m.runtimeClass, value)))

  private class MyLoop extends scala.tools.nsc.interpreter.ILoop(None, pw) {

    override protected def postInitialization() {
      super.postInitialization()
      bindSettings()
    }

    /** Bind the settings so that evaluated code can modify them */
    def bindSettings() {
      intp beQuietDuring {
        for ((name, (clazz, value)) <- bindings) {
          intp.bind(name, clazz.getCanonicalName, value)
        }
        initCmds.foreach(command)
      }
    }
  }

  protected def onExit() {
    TerminalFactory.get.setEchoEnabled(true)
  }

  def run() {
    val il = new MyLoop
    il.setPrompt(name + "> ")
    // val settings = new scala.tools.nsc.Settings()
    val settings = new GenericRunnerSettings(pw.println)
    //settings.embeddedDefaults(getClass.getClassLoader)
    settings.usejavacp.value = true

    Runtime.getRuntime().addShutdownHook(new Thread() {
      override def run() {
        // Jline apparently leaves things messed up
        TerminalFactory.get.setEchoEnabled(true)
      }
    });

    il process settings

    onExit()
  }

}