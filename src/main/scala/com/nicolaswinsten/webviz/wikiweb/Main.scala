package com.nicolaswinsten.webviz.wikiweb

import com.nicolaswinsten.webviz.WebView

object Main extends App {

  def stringToPage(s: String): Option[Page] =
    try Some(Page(s)) catch {
      case _: Exception => None
  }

  new WebView(stringToPage, 1500, 900).main(args)
}
