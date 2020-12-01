package com.nick.webviz

import java.awt.Font

import Physics._

import scala.annotation.tailrec
import scala.collection.mutable
import scala.swing.Color
import scala.util.Random

class Web(width: Int, height: Int) { // TODO add bounds to web or at least make it canvas scrollable somehow
  /**
   * Maps NodeLikes in this web to their position vector in the web
   */
  val nodes: mutable.Map[NodeLike, Vector2] = mutable.HashMap[NodeLike, Vector2]()
  /**
   * Set of all the arcs present in this web connecting NodeLikes
   */
  val arcs: mutable.Set[Arc] = mutable.HashSet[Arc]()

  /**
   * Nodes will be repelled if they are closer than this distance.
   * Nodes connected by arcs will be attracted if the arc length is greater than this distance
   */
  private val prefNodeDist = 150

  /**
   * Add a new Node to this Web. If the given node has a
   * relationship to any pre-existing node, then construct arc between them
   * @param node NodeLike to add
   * @param pos position in Web to place this node
   */
  def add(node: NodeLike, pos: Vector2): Unit = {
    if (!nodes.contains(node)) {
      nodes += (node -> pos)// add NodeLike to position in web
      for ((existingNode, _) <- nodes) { // find relationships among simulated nodes
        if (existingNode.children(node)) arcs += Arc(existingNode, node)
        if (node.children(existingNode)) arcs += Arc(node, existingNode)
      }
    }
  }

  /**
   * Add given node to center of web
   * @param node NodeLike to add to this web
   */
  def add(node: NodeLike): Unit = add(node, Vector2(0,0))

  /**
   * Remove the given NodeLike from the web along with its adjacent Arcs
   * @param node NodeLike to remove
   */
  def rem(node: NodeLike): Unit = {
    for (a <- arcs if  a.source == node || a.dest == node) arcs -= a
    nodes -= node
  }

  /**
   * Add all the given NodeLike's children to the Web, but only if the given NodeLike is already in the Web
   * @param n NodeLike to expand web with
   */
  def spin(n: NodeLike): Unit = if (nodes contains n) n.children.foreach(add(_, nodes(n))) else println(s"$n aint here mf")

  /**
   * Add all the given NodeLike's parents to the Web, but only if the given NodeLike is already in the Web
   * @param n NodeLike to expand web with
   */
  def climb(n: NodeLike): Unit = if (nodes contains n) n.parents.foreach(add(_, nodes(n))) else println(s"$n aint here mf")
  // TODO add collapse on a NodeLike that will remove the NodeLike and any nodes that are singly connected to it
  def collapse(n: NodeLike): Unit = {
    if (nodes contains n) {
      arcs filter (a => a.source == n || a.dest == n) map {
        case Arc(`n`, other) => other
        case Arc(other, `n`) => other
      } filter (other => degree(other) < 2) foreach rem
      rem(n)
    }
  }


  /**
   * Update positions of Nodes in the web
   */
  def update(): Unit = {
    arcs.foreach(_.pull()) // have each arc pull their nodes together

    // brute force a collision detection between nodes
    for ((n1,pos1) <- nodes) for ((n2,pos2) <- nodes) if (n1 != n2 && dist(pos1,pos2) < prefNodeDist) {
      val force = if (pos1 == pos2) {
        // if the two Nodes are directly on top of each other, push them in a random direction
        val r = Random
        def randCoord = (r.nextInt % 20) * (if (r.nextBoolean()) 1 else -1)
        Vector2(randCoord,randCoord)
      }  else // repel Nodes in opposite directions
        (pos2 - pos1) * (10 / math.pow(dist(pos1, pos2), 2))

      nodes(n1) -= force
      nodes(n2) += force

      nodes(n1) = bound(nodes(n1))
      nodes(n2) = bound(nodes(n2))

      /**
       * Given a position that is outside the Web, return a new position vector that is on the bounds of the Web
       *
       * @param pos position that is possibly out of bounds
       * @return new position that is in bounds
       */
      @tailrec
      def bound(pos: Vector2): Vector2 = {
        val x = pos.x
        val y = pos.y

        if (x < -width/2) bound(Vector2(-width/2, y))
        else if (x > width/2) bound(Vector2(width/2, y))
        else if (y < -height/2) bound(Vector2(x, -height/2))
        else if (y > height/2) bound(Vector2(x, height/2))
        else pos
      }

    }
  }

  /**
   * @param n NodeLike in this Web
   * @return number of arcs connected to this NodeLike in this Web
   */
  def degree(n: NodeLike): Int = arcs count (a => a.source == n || a.dest == n)

  /**
   * Return the NodeLike near the given coordinate vector in this Web
   * @param coords position in web
   * @param maxDist maximum distance the returned node can be from the given coords
   * @return Some(NodeLike) near the coords, None if there is no Node
   */
  def getNodeAt(coords: Vector2, maxDist: Int): Option[NodeLike] = {
    for ((n, pos) <- nodes if dist(coords, pos) <= maxDist) { return Some(n)}
    None
  }
  /**
   * Debugging print
   */
  def print(): Unit =  {
    for (n <- nodes) println(n)
    for (a <- arcs) println(s"arc from ${a.source} to ${a.dest} with length ${a.length}")
  }


  /**
   * Trait for a directed edge that links two Nodes together
   */
  case class Arc(source: NodeLike, dest: NodeLike) {
    val strength: Double = 0.001

    /**
     * @return physical length of this Arc determined by the distance between its incident Nodes
     */
    def length: Double = dist(nodes(source), nodes(dest))
    /**
     * Pull incident nodes together by modifying their collider position
     */
    def pull(): Unit = {
      val sourcePos = nodes(source)
      val destPos = nodes(dest)

      val force: Vector2 =
        if (length > prefNodeDist) {
          (destPos - sourcePos) * strength
        } else Vector2(0,0)

      nodes(source) += force
      nodes(dest) -= force
    }
  }
}

/**
 * NodeLike objects can be inserted into a Web. When extending this trait, creating an immutable type is best.
 * The equals and hashcode methods should NOT give different output depending on the when it is called in the
 * object's lifetime.
 */
trait NodeLike {
  protected val _label: String
  val children: Set[NodeLike]
  val parents: Set[NodeLike]

  def label: String = _label

  val textColor: Color
  val font: Font

  def specialAction()
}

/**
 * NodeFactory that can take in String and return a NodeLike
 */
trait NodeFactory {
  def produceNode(s: String): NodeLike
}



/**
 * Physics stuff
 */
object Physics {
  /**
   * Vector with 2 components
   */
  case class Vector2(x: Double, y: Double) {
    def +(v: Vector2): Vector2 = Vector2(this.x + v.x, this.y + v.y)
    def -(v: Vector2): Vector2 = Vector2(this.x - v.x, this.y - v.y)
    def *(m: Double): Vector2 = Vector2(this.x * m, this.y * m)
    def unary_- : Vector2 = Vector2(-this.x, -this.y)
  }

  /**
   * @return Euclidean distance between two points in 2D space
   */
  def dist(a: Vector2, b: Vector2): Double =
    math.sqrt(math.pow(a.x-b.x,2) + math.pow(a.y-b.y,2))
}