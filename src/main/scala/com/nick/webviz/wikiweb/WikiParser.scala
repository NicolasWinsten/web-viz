package com.nick.webviz.wikiweb

import net.ruippeixotog.scalascraper.browser.{Browser, JsoupBrowser}
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Document

object WikiParser {
  val browser: Browser = JsoupBrowser()
  val url = "https://en.wikipedia.org/"

  /**
   * @param title wikipedia title including namespace
   * @return HTML document of the page of the given wikipedia title
   */
  def fetchHTML(title: String): browser.DocumentType = browser.get(url + "/wiki/" + title)

  /**
   * Extracts the normal categories from a given Wikipedia page
   * @param doc HTML Document for the Wikipedia page
   * @return category titles
   */
  def getCategories(doc: Document): Seq[String] =
    doc >?> element("#mw-normal-catlinks") match {
      case Some(x) => x >> elementList("li > a") >> attr("title")
      case None => Nil
    }

  /**
   * Extracts all the subcategories on a Category page.
   * @param doc HTML Document of Wikipedia Category page
   * @return subcategory titles
   */
  def getSubcategories(doc: Document): Seq[String] = {
    doc >?> element("#mw-subcategories") match {
      case Some(x) => x >> elementList("div.CategoryTreeItem > a") >> attr("title")
      case None => Nil
    }
  }

  /**
   * Extract the members from a given Wikipedia Category page
   * @param doc HTML Document of Wikipedia Category page
   * @return category member titles
   */
  def getCategoryMembers(doc: Document): Seq[String] = {
    val firstMembers = doc >?> element("#mw-pages .mw-content-ltr") match {
      case Some(x) => x >> elementList("a") >> attr("title")
      case None => Nil
    }

    // Category pages with a lot of members will contain a link to a "next page" with more
    // follow the link to the next page to get more category members
    val link = ((doc >> elementList("#mw-pages > a")) find (a => a.text == "next page")) >?> attr("href")
    println(link)
    println(firstMembers)
    link match {
      case Some(Some(link)) => firstMembers ++: getCategoryMembers(browser.get(url + link.stripPrefix("/")))
      case _ => firstMembers
    }
  }


}
