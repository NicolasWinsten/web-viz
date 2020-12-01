import java.awt.{Color, Font}
import java.net.{URI, URL, URLEncoder}

import WikiParser.browser

import scala.swing.Color

sealed abstract class Page(val title: String) extends NodeLike {
  lazy val doc: browser.DocumentType = WikiParser.fetchHTML(title)
  lazy val categories: Seq[Category] = Wiki.filterDisambs(WikiParser.getCategories(doc)) map (Category(_))

  lazy override val parents: Set[NodeLike] = categories.toSet

  lazy val fixedTitle = title.toUpperCase()

  override def equals(obj: Any): Boolean = obj match {
    case other: Page => other.fixedTitle == fixedTitle
    case _ => false
  }

  override def hashCode(): Int = fixedTitle.hashCode

  override def specialAction() = Wiki.openWebpage(this)

}

case class Article(override val title: String) extends Page(title) {
  override val _label: String = title
  /**
   * The empty set. Article pages have no children
   */
  lazy override val children: Set[NodeLike] = Set()

  override val textColor: Color = Color.BLACK
  override val font = new Font("TimesRoman", Font.ITALIC, 10)
}
case class Category(override val title: String) extends Page(title) {
  lazy val members: Seq[Article] = Wiki.filterDisambs(WikiParser.getCategoryMembers(doc)) map (Article(_))
  lazy val subcategories: Seq[Category] = Wiki.filterDisambs(WikiParser.getSubcategories(doc)) map (Category(_))

  override val _label: String = title.stripPrefix("Category:")
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
  def filterDisambs(titles: Seq[String]) =
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

