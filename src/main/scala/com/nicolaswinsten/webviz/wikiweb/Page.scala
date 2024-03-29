package com.nicolaswinsten.webviz.wikiweb

import java.awt.Color
import java.net.{URI, URL}

import com.nicolaswinsten.webviz.wikiweb.WikiParser.browser
import com.nicolaswinsten.webviz.NodeLike

import scala.swing.Font

/**
 * Abstract class for a Wikipedia page to be used as a node in a Web.
 * @param title title for the page
 */
sealed abstract class Page(val title: String) extends NodeLike {
  /**
   * HTML Document of this Wikipedia Page
   */
  lazy val doc: browser.DocumentType = WikiParser.fetchHTML(title)
  /**
   * The Category pages that this Page is a member of
   */
  lazy val categories: Seq[Category] = filterDisambs(WikiParser.getCategories(doc)) map Category

  /**
   * To be a NodeLike, we let the parents of a Page be its encompassing Categories
   */
  lazy override val parents: Set[NodeLike] = categories.toSet

  /**
   * Wikipedia is full of redirects which makes comparing Pages difficult because Page("United States")
   * and Page("USA") should be considered the same but there's no good way to efficiently confirm that without
   * fetching the HTML doc and extracting the webpage's title. However, a lot of redirects are the same
   * spelling but with different capitalization, so we can make a decent equals method by just setting our
   * title to all upper.
   */
  protected lazy val fixedTitle: String = title.toUpperCase

  /**
   * Set default repulsion constant to 10
   * @param other Node to push away
   *  @return multiplying constant applied to repulsion force from this Node on <var>other</var> Node
   */
  override def repel(other: NodeLike): Double = 10

  override def prefDist(other: NodeLike): Double = 150

  override def equals(obj: Any): Boolean = obj match {
    case other: Page => other.fixedTitle == fixedTitle
    case _ => false
  }

  override def hashCode(): Int = fixedTitle.hashCode

  /**
   * Open this Page in the browser
   */
  override def specialAction(): Unit = openWebpage(this)


  /**
   * Filter disambiguation titles from the given title strings
   * @param titles Wikipedia page titles
   * @return titles without disambiguation titles
   */
  protected final def filterDisambs(titles: Seq[String]): Seq[String] =
    titles filter (c => !c.toUpperCase.contains("DISAMBIGUATION"))

  import java.awt.Desktop
  import java.net.URISyntaxException

  /**
   * Open the webpage of the given URI in the Default browser
   * @param uri URI to open
   * @return true if successful, false if problem occurred
   */
  private final def openWebpage(uri: URI): Boolean = {
    val desktop = if (Desktop.isDesktopSupported) Desktop.getDesktop
    else null
    if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) try {
      desktop.browse(uri)
      return true
    } catch {
      case e: Exception =>
        e.printStackTrace()
    }
    false
  }

  /**
   * Open the wikipedia page in the default browser of the given Page
   * @param page Page to open in browser
   * @return true if no problems occurred, false otherwise
   */
  private final def openWebpage(page: Page): Boolean = {
    val url = new URL((WikiParser.url + "wiki/" + page.title).replaceAll(" ", "_"))
    try return openWebpage(url.toURI)
    catch {
      case e: URISyntaxException =>
        e.printStackTrace()
    }
    false
  }
}

/**
 * An Article page is a Wikipedia page in the MAIN namespace. In other words it is a normal wikipedia article,
 * not a Category or Talk or Portal.
 * @param title title of the page
 */
case class Article(override val title: String) extends Page(title) {
  override val label: String = title
  /**
   * The empty set. Article pages have no children
   */
  lazy override val children: Set[NodeLike] = Set()

  override val textColor: Color = if (fixedTitle == "KEVIN BACON") Color.MAGENTA else Color.BLACK
  override val font: Font = if (fixedTitle == "KEVIN BACON") Font("Helvetica", Font.Italic, 40)
    else Font("TimesRoman", Font.Italic, 10)
}

/**
 * A Category page from Wikipedia. Category pages have subcategories and category members.
 * @param title title for the page
 */
case class Category(override val title: String) extends Page(title) {
  lazy val members: Seq[Article] = filterDisambs(WikiParser.getCategoryMembers(doc, 10)) map Article
  lazy val subcategories: Seq[Category] = filterDisambs(WikiParser.getSubcategories(doc)) map Category

  // strip "Category" from label
  override val label: String = title.stripPrefix("Category:")
  /**
   * Let the children of a Category be its members and subcategories
   */
  lazy override val children: Set[NodeLike] = (subcategories ++: members).toSet

  override val font: Font = Font("TimesRoman", Font.Bold, 15)

  /**
   * The repulsion force between two category Pages is twice the default. This is so Category
   * pages will spread out better.
   * @param other Node to push away
   *  @return multiplying constant applied to repulsion force from this Node on <var>other</var> Node
   */
  override def repel(other: NodeLike): Double = other match {
    case other: Category => super.repel(other) * 15
    case other => super.repel(other)
  }

  /**
   * @param other another NodeLike
   *  @return preferred distance between this and <var>other</var>
   */
  override def prefDist(other: NodeLike): Double = other match {
    case other: Category => super.prefDist(other) * 5 // category pages prefer to be further apart
    case other => super.prefDist(other)
  }
}

object Page {
  /**
   * Page factory given a Wikipage title
   * @param title title to a Wikipedia page
   * @return the Page object for that title
   */
  def apply(title: String): Page = {
    val resolvedTitle = resolveTitle(title)
    if (resolvedTitle startsWith "Category:") Category(resolvedTitle)
    else Article(resolvedTitle)
  }

  /**
   * If the given title is a redirect, return the title it redirects to.
   * Otherwise return the title unchanged. If the given title does not exist, return None<br>
   *   Example: resolveTitle("USA") = "United States"
   * @param title Wikipedia page title to resolve
   * @return resolved Wikipedia title
   */
  private final def resolveTitle(title: String): String =
    try WikiParser.fetchHTML(title).title.stripSuffix(" - Wikipedia")
    catch {
      case _: Exception => throw new RuntimeException(s"Something went wrong resolving title $title")
  }

}
