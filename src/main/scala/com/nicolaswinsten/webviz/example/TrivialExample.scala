package com.nicolaswinsten.webviz.example

import java.awt.Font

import com.nicolaswinsten.webviz.NodeLike

import scala.swing.Color
import scala.util.Random

// trivial example of a NodeLike. It is best to use case classes for these.
// it will probable be necessary to include a good hashCode and equals method
case class TrivialExample(x: Int) extends NodeLike {
  // the label for the node to display
  override val label: String = x.toString

  // the children nodes are the two integers after x
  lazy override val children: Set[NodeLike] = Set(TrivialExample(x+1), TrivialExample(x+2))

  // the parent nodes are the two preceding numbers of x
  lazy override val parents: Set[NodeLike] = Set(TrivialExample(x-1), TrivialExample(x-2))

  // use a random text color for the nodes
  private val r = new Random
  override val textColor: Color = new Color(r.nextInt(255), r.nextInt(255), r.nextInt(255))

  // font to use for node label
  override val font: Font = new Font("ComicSans", Font.BOLD, 30)

  // these nodes have special action does nothing
  // so nothing will happen when middle mouse clicking on a node
  override def specialAction(): Unit = ()

  // set repulsion constant. this determines magnitude that nodes repel each other
  override def repel(other: NodeLike): Double = 10

  // set the preferred distance between nodes to a constant 100
  override def prefDist(other: NodeLike): Double = 100
}
