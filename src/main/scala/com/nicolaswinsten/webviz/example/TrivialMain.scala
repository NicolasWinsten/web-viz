package com.nicolaswinsten.webviz.example

import com.nicolaswinsten.webviz.WebView

object TrivialMain extends App {
  new WebView(TrivialExampleFactory, 1500, 900).main(args)
}
