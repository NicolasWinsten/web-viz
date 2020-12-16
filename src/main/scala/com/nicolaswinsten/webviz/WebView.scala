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
  // initialize center to center of canvas
  private var _center = Vector2(WIDTH / 2, HEIGHT / 2)

  /**
   * Reset the center of the canvas to a new point
   * @param v
   */
  def setCenter(v: Vector2) = _center = v

  /**
   * @return center of canvas
   */
  def center = _center

  // construct new web for this gui
  val web = new Web()
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
    listenTo(canvas.mouse.moves)
    listenTo(canvas.keys)

    // initialize drag position. value doesn't matter since it is reset each time mouse is pressed
    private var previousDragPos: Vector2 = center

    reactions += {
          // text command entered into text field
      case KeyPressed(_, key, _, _) if key == Key.Enter =>
        handleCommand(textField.text)
        textField.text = ""

      case KeyPressed(_, key, _, _) if key == Key.Space =>
        web.print()
        // user clicked somewhere on canvas
      case MouseClicked(_, point, modifiers, clicks, _) =>
        if (modifiers == 256) showActions(point) // right click
        else if (clicks == 2) web.getNodeAt(canvas.toWebPos(point), 20) match {
          case Some(node) => node.specialAction() // double click
          case _ => ()
        }
        else println(s"${canvas.toWebPos(point)}")
        // user scrolled mouse wheel. scale the canvas to zoom
      case MouseWheelMoved(_, _, _, rotation) => canvas.setScale(canvas.scale - rotation * 0.1 * canvas.scale)
        // user presses mouse, initialize drag position in case user tries to pan the canvas
      case MousePressed(_, point, _, _, _) => previousDragPos = Vector2(point.x, point.y)
        // user is dragging mouse, pan canvas accordingly by translating the center
      case MouseDragged(_, point, _) => {
        setCenter(center - (previousDragPos - Vector2(point.x, point.y))/canvas.scale)
        previousDragPos = Vector2(point.x, point.y)
      }
    }

    /**
     * Display the buttons that perform actions on the Node at (x,y) on the canvas,
     * or other non-node actions if there wasn't a node at that location
     * @param point Point on canvas
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
    private var _scale = 1.0
    private val minScale = 0.25
    private val maxScale = 3.0

    def scale: Double = _scale

    def setScale(x: Double): Unit =
      _scale = if (x < minScale) minScale else if (x > maxScale) maxScale else x

    /**
     * Paint the current state of the Web on this Canvas
     * @param g graphics context
     */
    override def paintComponent(g: Graphics2D) {
      g.clearRect(0, 0, size.width, size.height)

      g.translate(size.width/2, size.height/2)
      g.scale(_scale, _scale) // rescale canvas for zooming
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

    def toWebPos(point: Point): Vector2 =
      ((Vector2(point.x, point.y)-Vector2(WIDTH/2, HEIGHT/2))/scale - center) + Vector2(WIDTH/2, HEIGHT/2)
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


