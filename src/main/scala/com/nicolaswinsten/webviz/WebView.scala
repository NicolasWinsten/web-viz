package com.nicolaswinsten.webviz

import com.nicolaswinsten.webviz.Physics.Vector2

import scala.swing._

/**
 * GUI View of a new Web
 * @param stringToNode function that produces a Node from a String
 */
class WebView(stringToNode: String => Option[NodeLike], private val WIDTH: Int, private val HEIGHT: Int)
  extends SimpleSwingApplication {
  override def top: Frame = new MainFrame{
    title = "Web"

    val web = new Web()

    val canvas = new WebCanvas(stringToNode, web) {
      preferredSize = new Dimension(WIDTH,HEIGHT)
    }

    contents = canvas

    // initialize web with nodes that give instructions to user
    instructionNodes foreach { web.add(_, Vector2(canvas.size.width/2, canvas.size.height/2)) }
  }

  val instructionNodes = List(
    new LoneNode("Right click on canvas to add a Node"),
    new LoneNode("Right click on nodes to explore them further"),
    new LoneNode("Double click on a Node to execute its special action")
  )

  /**
   * LoneNodes are Nodes that cannot be connected to any other Nodes.
   * They are used here just to display instructions in the WebView.
   * @param label String text for node
   */
  class LoneNode(override val label: String) extends NodeLike {
    override val children: Set[NodeLike] = Set()
    override val parents: Set[NodeLike] = Set()
  }
}


