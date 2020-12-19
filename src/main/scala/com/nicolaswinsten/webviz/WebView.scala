package com.nicolaswinsten.webviz

import scala.swing._

/**
 * GUI View of a new Web
 * @param stringToNode function that produces a Node from a String
 */
class WebView(stringToNode: String => NodeLike, private val WIDTH: Int, private val HEIGHT: Int)
  extends SimpleSwingApplication {
  override def top: Frame = new MainFrame{
    title = "Web"
    contents = new WebCanvas(stringToNode, new Web()) {
      preferredSize = new Dimension(WIDTH,HEIGHT)
    }
  }
}


