package com.nicolaswinsten.webviz.example

import com.nicolaswinsten.webviz.WebView

object TrivialMain extends App {
  def sToIntNode(s: String) = {
    try {
      Some(TrivialExample(s.toInt))
    }
    catch {
      case _: NumberFormatException => None
    }
  }

  new WebView(sToIntNode, 1500, 900).main(args)
}
