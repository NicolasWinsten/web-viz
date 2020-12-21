package com.nicolaswinsten.webviz.wikiweb

import com.nicolaswinsten.webviz.WebView

object Main extends App {
  new WebView(Page(_), 1500, 900).main(args)
}
