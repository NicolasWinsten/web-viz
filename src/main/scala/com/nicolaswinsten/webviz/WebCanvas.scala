package com.nicolaswinsten.webviz

import java.awt.image.BufferedImage
import java.awt.Color
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar

import com.nicolaswinsten.webviz.Physics.Vector2
import javax.imageio.ImageIO
import javax.swing.SwingUtilities

import scala.swing.{Action, Button, Component, Dimension, Graphics2D, Label, Panel, Point, PopupMenu, TextField}
import scala.swing.event.{MouseClicked, MouseDragged, MousePressed, MouseWheelMoved}

/**
 * Panel that the web is drawn on.
 * @param stringToNode String to NodeLike function
 * @param web Web to draw on this canvas
 */
private class WebCanvas(private val stringToNode: String => Option[NodeLike], private val web: Web) extends Panel {
  /**
   * Construct new WebCanvas with an empty Web
   * @param stringToNode String => Node producer
   */
  def this(stringToNode: String => Option[NodeLike]) = this(stringToNode, new Web())

  private var _scale = 1.0
  private val minScale = 0.25
  private val maxScale = 3.0

  private def scale: Double = _scale

  /**
   * Reset the zoom level of web drawing
   * @param x new zoom level
   */
  private def setScale(x: Double): Unit =
    _scale = if (x < minScale) minScale else if (x > maxScale) maxScale else x


  // initialize center to center of canvas
  private var _center = canvasCenter

  /**
   * Reset the center of the drawing to a new point
   * @param v new center
   */
  private def setCenter(v: Vector2): Unit = _center = v

  /**
   * @return center of drawing
   */
  private def center: Vector2 = _center

  /**
   * @return point at center of window
   */
  private def canvasCenter: Vector2 = Vector2(size.width/2, size.height/2)

  /**
   * if saveOnPaint is true, then a PNG image containing the web will be written when canvas.repaint() is next called
   */
  private var saveOnPaint = false

  /**
   * Paint the current state of the Web on this Canvas
   * @param g graphics context
   */
  override def paintComponent(g: Graphics2D) {
    // draw the the Web onto a given Graphics2D object
    // w and h are the width and height of the drawing area
    def drawWebOnGraphics2D(g: Graphics2D, w: Int, h: Int): Unit = {
      g.setBackground(Color.WHITE)
      g.clearRect(0, 0, w, h)

      g.translate(size.width / 2, size.height / 2)
      g.scale(_scale, _scale) // rescale canvas for zooming
      g.translate(-size.width / 2, -size.height / 2)

      for (a <- web.arcs) drawArc(a)
      for ((n, _) <- web.nodes) drawNode(n)

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

    drawWebOnGraphics2D(g, size.width, size.height) // draw web on this canvas
    if (saveOnPaint && !web.isEmpty) { // if saveOnPaint has been toggled, draw web to new PNG
      val originalCenter = _center
      val originalScale = _scale

      // grab current bounds of the web
      val (minX, minY, maxX, maxY) = web.bounds

      // briefly reset scale and center canvas
      _scale = 1.0 // set scale back to normal, so that everything is readable
      _center = Vector2(-minX + 100, -minY + 100) // center canvas to capture all nodes

      val bf = new BufferedImage((maxX - minX).toInt + 200, (maxY - minY).toInt + 200, BufferedImage.TYPE_INT_RGB)
      drawWebOnGraphics2D(bf.createGraphics(), bf.getWidth, bf.getHeight)
      val timeStr = new SimpleDateFormat("MM_dd_YYYY_hms").format(Calendar.getInstance.getTime)
      ImageIO.write(bf, "png", new File(s"${web.centralNode.label}$timeStr.png"))
      saveOnPaint = false

      // revert canvas scale and center
      _scale = originalScale
      _center = originalCenter
    }
  }

  /**
   * Translate a point on the canvas window to a position in the Web
   * @param point Point in window
   * @return position of that point in the Web
   */
  private def toWebPos(point: Point): Vector2 =
    ((Vector2(point.x, point.y)-canvasCenter)/scale - center) + canvasCenter

  listenTo(mouse.clicks) // click on nodes
  listenTo(mouse.moves)  // pan image by dragging
  listenTo(mouse.wheel)  // zoom image by scrolling mouse wheel

  // initialize drag position. value doesn't matter since it is reset each time mouse is pressed
  private var previousDragPos: Vector2 = center

  reactions += {
    case MouseClicked(_, point, 256, _, _) => showActions(point) // right click
    case MouseClicked(_, point, _, 2, _) => web.getNodeAt(toWebPos(point), 20) match {
        // double click on nodes to activate their special action
      case Some(node) => node.specialAction()
      case None => ()
    }
    // user scrolled mouse wheel. scale the canvas to zoom
    case MouseWheelMoved(_, _, _, rotation) => setScale(scale - rotation * 0.1 * scale)
    // user presses mouse, initialize drag position in case user tries to pan the canvas
    case MousePressed(_, point, _, _, _) => previousDragPos = Vector2(point.x, point.y)
    // user is dragging mouse, pan canvas accordingly by translating the center
    case MouseDragged(_, point, _) =>
      setCenter(center - (previousDragPos - Vector2(point.x, point.y)) / scale )
      previousDragPos = Vector2(point.x, point.y)
  }

  /**
   * Display the buttons that perform actions on the Node at (x,y) on the canvas,
   * or other non-node actions if there wasn't a node at that location
   * @param point Point on canvas
   */
  private def showActions(point: Point): Unit =
    web.getNodeAt(toWebPos(point), 20) match {
      case Some(node) => new PopupMenu {
        // create buttons for PopupMenu
        contents += new Button( Action("Show Children"){ web.spin(node); this.visible = false } )
        contents += new Button( Action("Show Parents"){ web.climb(node); this.visible = false } )
        contents += new Button( Action("Remove"){ web.rem(node); this.visible = false } )
        contents += new Button( Action("Collapse"){ web.collapse(node); this.visible = false } )
        SwingAligner.unifyWidth(contents)
      }.show(this, point.x, point.y)
      case None => new PopupMenu { // show option to clear web or add new Node if there was no Node at (x,y)
        def removeMenu(): Unit = this.visible = false // close this popup menu
        val clearWebButton = new Button( Action("Clear Web"){ web.clear(); removeMenu()})
        val addNodeTextField: TextField = new TextField {
          action = Action("Add Node"){
          // convert the text in this field into a Node and add it to web
            stringToNode(text) match {
              case Some(node) =>
                web.add(node, toWebPos(point))
                removeMenu()
              case None => ()
            }
          }
          preferredSize = new Dimension(100 ,preferredSize.height)
        }
        contents += new Label("Enter Node:")
        contents += addNodeTextField
        if (!web.isEmpty) { // only show Clear Web and Export PNG if Web has nodes
          contents += clearWebButton
          contents += new Button( Action("Export PNG"){ saveOnPaint = true; removeMenu() })
        }
        SwingAligner.unifyWidth(contents)
      }.show(this, point.x, point.y)
    }

  /**
   * Start redrawing loop to continuously update the web state and draw it on the canvas
   */
  private def redraw(): Unit = {
    web.update()
    repaint()
    SwingUtilities.invokeLater(()=> redraw())
  }
  redraw()
}

object SwingAligner {
  /**
   * Change the given components such that their widths match the maximum size out of all of the components
   * @param components Swing components
   */
  def unifyWidth(components: Iterable[Component]): Unit = {
    val maxWidth = components map { _.maximumSize } maxBy { _.width }
    components foreach { _.maximumSize = maxWidth }
  }
}
