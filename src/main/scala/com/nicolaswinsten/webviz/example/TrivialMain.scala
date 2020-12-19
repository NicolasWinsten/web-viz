package com.nicolaswinsten.webviz.example

import com.nicolaswinsten.webviz.WebView

object TrivialMain extends App {
  new WebView((s: String) => TrivialExample(s.toInt), 1500, 900).main(args)
}
