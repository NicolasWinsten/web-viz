package com.nick.webviz

import java.awt.Color

import Physics._
import javax.swing.SwingUtilities

import scala.swing.BorderPanel.Position._
import scala.swing._
import scala.swing.event._

/**
 * GUI View of a new Web
 */
class WebView(nodeFactory: NodeFactory, private val WIDTH: Int, private val HEIGHT: Int)
  extends SimpleSwingApplication {
  def center: Vector2 = Vector2(WIDTH / 2, HEIGHT / 2)

  // construct new web for this gui
  val web = new Web(WIDTH-15, HEIGHT-15)
  // Give web slightly smaller bounds to reduce issue where Node labels are off screen

  // define our top frame
  val frame: MainFrame = new MainFrame{
    title = "Web"
    val canvas: Canvas = new Canvas {
      preferredSize = new Dimension(WIDTH,HEIGHT)
    }
    val textField = new TextField()

    contents = new BorderPanel {
      layout(canvas) = Center
      layout(textField) = South
    }

    listenTo(textField.keys)
    listenTo(canvas.mouse.clicks)

    reactions += { // listen to Enter hits on text field and mouse clicks on canvas
      case KeyPressed(_, key, _, _) if key == Key.Enter =>
        handleCommand(textField.text)
        textField.text = ""
      case MouseClicked(_, point, modifiers, _, _) => handleClick(Vector2(point.x, point.y), modifiers)
    }

    /**
     * Start redrawing loop to continuously update the web state and draw it on the canvas
     */
    def redraw(): Unit = {
      Thread.sleep(10)
      web.update()
      canvas.repaint()
      SwingUtilities.invokeLater(()=> redraw())
    }
    redraw()
  }

  // assign our frame
  override def top: Frame = frame

  /**
   * Panel that the web is drawn on
   */
  class Canvas extends Panel {

    /**
     * Paint the current state of the Web on this Canvas
     * @param g graphics context
     */
    override def paintComponent(g: Graphics2D) {
      g.clearRect(0, 0, size.width, size.height)

      for (a <- web.arcs) drawArc(a)
      for ((n,_) <- web.nodes) drawNode(n)

      def drawArc(arc: web.Arc): Unit = {
        g.setColor(Color.BLACK)
        val startPoint = web.nodes(arc.source) + center
        val endPoint = web.nodes(arc.dest) + center
        g.drawLine(startPoint.x.toInt, startPoint.y.toInt, endPoint.x.toInt, endPoint.y.toInt)
      }

      def drawNode(node: NodeLike): Unit = {
        val pos = web.nodes(node) + center
        val metrics = g.getFontMetrics(node.font)
        // clear a rectangle behind the node text so that node label is not covered by drawn arcs
        g.clearRect(pos.x.toInt - metrics.stringWidth(node.label)/2, pos.y.toInt - metrics.getHeight/4,
          metrics.stringWidth(node.label), metrics.getHeight)
        // draw node label
        g.setColor(node.textColor)
        g.setFont(node.font)
        g.drawString(node.label, pos.x.toInt - metrics.stringWidth(node.label)/2, pos.y.toInt + metrics.getHeight/2)
      }
    }
  }

  /**
   * Handle the command typed in by the user.
   * @param command user command with the first word being the operation and
   *                the rest of the string being its one argument
   */
  private def handleCommand(command: String): Unit = {
    val args = command.split(" ")

    val cmd = args(0)
    val arg = args.slice(1, args.length).mkString(" ")

    cmd.toUpperCase match {
      case "ADD" => web.add(nodeFactory.produceNode(arg))
      case "REMOVE" | "REM" | "DELETE" | "DEL" => web.rem(nodeFactory.produceNode(arg))
      case "SPIN" => web.spin(nodeFactory.produceNode(arg))
      case "CLIMB" => web.climb(nodeFactory.produceNode(arg))
      case "COLLAPSE" => web.collapse(nodeFactory.produceNode(arg))
      case "HELP" => Dialog.showMessage(frame, helpMessage, "How to Use Web")
      case _ => println(s"$command not valid command")
    }
  }

  private def handleClick(pos: Physics.Vector2, modifiers: Key.Modifiers): Unit = {
    val webPos = pos - center // pos of the click in web
    web.getNodeAt(webPos, 20) match {
      case Some(node) => modifiers match {
        case 0 => web.spin(node)    // left click
        case 256 => web.climb(node) // right click
        case 64 => web.rem(node)    // shift click
        case 128 => web.collapse(node) // ctrl + click
        case 512 => node.specialAction() // middle mouse click
        case _ =>
      }
      case _ => // no node found at the mouse click position
    }

  }

  /**
   * message to display in Dialog if user needs asks for help
   */
  private val helpMessage: String =
    """
      |Commands:
      |      add <Node> :  add the given Node to the web
      |     spin <Node> :  add the children of a pre-existing Node to the web
      |      (or left click on Node)
      |    climb <Node> :  add the parents of a pre-existing Node to the web
      |       (or right click on Node)
      |   delete <Node> :  remove the given Node from the web
      |       (or shift click on Node)
      |                    Aliases: del  remove  rem
      | collapse <Node> :  remove the given Node along with all its dangling children / parents
      |       (or ctrl + click on Node)
      |""".stripMargin


}


