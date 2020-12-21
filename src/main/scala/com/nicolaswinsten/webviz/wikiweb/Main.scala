package com.nicolaswinsten.webviz.wikiweb

import com.nicolaswinsten.webviz.WebView

object Main extends App {
  new WebView((s: String) => Page(s), 1500, 900).main(args)
}
