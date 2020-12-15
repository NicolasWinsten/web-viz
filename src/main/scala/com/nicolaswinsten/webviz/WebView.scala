package com.nicolaswinsten.webviz

import java.awt.{Color}

import Physics._
import javax.swing.SwingUtilities

import scala.swing.BorderPanel.Position._
import scala.swing._
import scala.swing.event._

/**
 * GUI View of a new Web
 * @param nodeFactory object that can produce NodeLikes from String
 */
class WebView(nodeFactory: NodeFactory, private val WIDTH: Int, private val HEIGHT: Int)
  extends SimpleSwingApplication {
  def center: Vector2 = Vector2(WIDTH / 2, HEIGHT / 2)

  // construct new web for this gui
  val web = new Web(WIDTH-15, HEIGHT-15)
  // Give web slightly smaller bounds to reduce issue where Node labels are off screen

  // define our top frame
  val frame: MainFrame = new MainFrame{
    title = "Web - type help"
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
    listenTo(canvas.mouse.wheel)

    reactions += { // listen to Enter hits on text field and mouse clicks on canvas
      case KeyPressed(_, key, _, _) if key == Key.Enter =>
        handleCommand(textField.text)
        textField.text = ""
      case MouseClicked(_, point, modifiers, clicks, _) =>
        if (modifiers == 256) showActions(point) // right click
        else if (clicks == 2) web.getNodeAt(canvas.toWebPos(point), 20) match {
          case Some(node) => node.specialAction() // double click
          case _ => ()
        }
        else println(canvas.toWebPos(point))
      case MouseWheelMoved(_, point, _, rotation) => canvas.scale -= rotation*0.1*canvas.scale
    }

    /**
     * Display the buttons that perform actions on the Node at (x,y) on the canvas,
     * or other non-node actions if there wasn't a node at that location
     * @param x x-position of Node on canvas
     * @param y y-position of Node on canvas
     */
    private def showActions(point: Point): Unit =
      web.getNodeAt(canvas.toWebPos(point), 20) match {
        case Some(node) => new PopupMenu {
            // create buttons for PopupMenu
            contents += new Button( Action("Show Children"){ web.spin(node); this.visible = false } )
            contents += new Button( Action("Show Parents"){ web.climb(node); this.visible = false } )
            contents += new Button( Action("Remove"){ web.rem(node); this.visible = false } )
            contents += new Button( Action("Collapse"){ web.collapse(node); this.visible = false } )

          // find maximum width out of the buttons
          val maxWidth = contents map { _.maximumSize } maxBy { _.width }
          // assign each button that width
          contents foreach { _.maximumSize = maxWidth }
        }.show(canvas, point.x, point.y)
        case None => new PopupMenu { // show option to clear web if there was no Node at (x,y)
          contents += new Button( Action("Clear Web"){ web.clear(); this.visible = false } )
        }.show(canvas, point.x, point.y)
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
    var scale = 1.0
    /**
     * Paint the current state of the Web on this Canvas
     * @param g graphics context
     */
    override def paintComponent(g: Graphics2D) {
      g.clearRect(0, 0, size.width, size.height)

      g.translate(size.width/2, size.height/2)
      g.scale(scale, scale) // rescale canvas for zooming
      g.translate(-size.width/2, -size.height/2)

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

    def toWebPos(point: Point) = (Vector2(point.x, point.y) - center) / scale
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
      case "CLEAR" => web.clear()
      case "HELP" => Dialog.showMessage(frame, helpMessage, "How to Use Web")
      case _ => println(s"$command not valid command")
    }
  }


  /**
   * message to display in Dialog if user needs asks for help
   */
  private val helpMessage: String =
    """
      |Type Command:
      |      add <Node> :  add new Node to the web
      |right click node to see actions
      |double click node for special action
      |""".stripMargin


}


