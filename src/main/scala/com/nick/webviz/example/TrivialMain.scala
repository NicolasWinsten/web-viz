package com.nick.webviz.example

import com.nick.webviz.WebView

object TrivialMain extends App {
  new WebView(TrivialExampleFactory, 1500, 900).main(args)
}
