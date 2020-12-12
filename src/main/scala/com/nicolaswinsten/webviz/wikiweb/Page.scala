package com.nicolaswinsten.webviz.wikiweb

import java.net.{URI, URL}

import com.nicolaswinsten.webviz.wikiweb.WikiParser.browser
import com.nicolaswinsten.webviz.{NodeFactory, NodeLike}

import java.awt.{Color, Font}

/**
 * Abstract class for a Wikipedia page.
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
  lazy val categories: Seq[Category] = Wiki.filterDisambs(WikiParser.getCategories(doc)) map (Category(_))

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
  protected lazy val fixedTitle = title.toUpperCase()


  override def equals(obj: Any): Boolean = obj match {
    case other: Page => other.fixedTitle == fixedTitle
    case _ => false
  }

  override def hashCode(): Int = fixedTitle.hashCode

  /**
   * Open this Page in the browser
   */
  override def specialAction(): Unit = Wiki.openWebpage(this)

}

/**
 * An Article page is a Wikipedia page in the MAIN namespace. In other words it is a normal wikipedia article,
 * not a Category or Talk or Portal.
 * @param title title of the page
 */
case class Article(override val title: String) extends Page(title) {
  override val _label: String = title
  /**
   * The empty set. Article pages have no children
   */
  lazy override val children: Set[NodeLike] = Set()


  override val textColor: Color = if (fixedTitle == "KEVIN BACON") Color.MAGENTA else Color.BLACK
  override val font: Font = if (fixedTitle == "KEVIN BACON") new Font("Helvetica", Font.ITALIC, 40)
    else new Font("TimesRoman", Font.ITALIC, 10)
}

/**
 * A Category page from Wikipedia. Category pages have subcategories and category members.
 * @param title title for the page
 */
case class Category(override val title: String) extends Page(title) {
  lazy val members: Seq[Article] = Wiki.filterDisambs(WikiParser.getCategoryMembers(doc, 10)) map (Article(_))
  lazy val subcategories: Seq[Category] = Wiki.filterDisambs(WikiParser.getSubcategories(doc)) map (Category(_))

  // strip "Category" from label
  override val _label: String = title.stripPrefix("Category:")
  /**
   * Let the children of a Category be its members and subcategories
   */
  lazy override val children: Set[NodeLike] = (subcategories ++: members).toSet


  override val textColor: Color = Color.BLACK
  override val font = new Font("TimesRoman", Font.BOLD, 15)
}

/**
 * Page factory
 */
object Wiki extends NodeFactory {
  /**
   * Page factory given a Wikipage title
   * @param title title to a Wikipedia page
   * @return the Page object for that title
   */
  def getPage(title: String): Page = title match {
    case title if title.startsWith("Category:") => Category(title)
    case title => Article(title)
  }

  /**
   * Filter disambiguation titles from the given title strings
   * @param titles Wikipedia page titles
   * @return titles without disambiguation titles
   */
  def filterDisambs(titles: Seq[String]): Seq[String] =
    titles filter (c => !c.toUpperCase.contains("DISAMBIGUATION"))

  import java.awt.Desktop
  import java.net.URISyntaxException

  /**
   * Open the webpage of the given URI in the Default browser
   * @param uri URI to open
   * @return true if successful, false if problem occurred
   */
  private def openWebpage(uri: URI): Boolean = {
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
  def openWebpage(page: Page): Boolean = {
    val url = new URL((WikiParser.url + "/wiki/" + page.title).replaceAll(" ", "_"))
    try return openWebpage(url.toURI)
    catch {
      case e: URISyntaxException =>
        e.printStackTrace()
    }
    false
  }

  /**
   * Produces the correct Page given a string title
   * @param s wikipage title
   * @return Page for that title
   */
  override def produceNode(s: String): NodeLike = getPage(s)
}

