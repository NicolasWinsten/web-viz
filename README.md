# web-viz
An interactive graph visualizer!
![Kevin Bacon](/KevinBaconViz.PNG)
![Spider web](/SpiderWebViz.PNG)
![Numbers](/NumbersViz.PNG)

web-viz is a tool that can allow you to interactively traverse whatever graph data you want!
The above two examples is what you get when you use Wikipedia articles and categories as your graph data.
Left/Right clicking a title will produce more nodes in the web that you can also click on.

# How to use:

Add this to your `build.sbt`:
```scala
// Scala Swing required for formatting your Nodes with Color and Font
libraryDependencies += "org.scala-lang.modules" %% "scala-swing" % "3.0.0"

externalResolvers += "web-viz packages" at "https://maven.pkg.github.com/NicolasWinsten/web"
libraryDependencies += "nicolaswinsten" %% "web-viz" % "1.0.2"
```

Next, you'll want to define your own node class extending `NodeLike`, and a `NodeFactory` class that can turn a String into one of your `NodeLike`s:
```scala
trait NodeLike {
  protected val _label: String
  val children: Set[NodeLike]
  val parents: Set[NodeLike]
  def label: String = _label
  val textColor: Color
  val font: Font
  def specialAction()
}

trait NodeFactory {
  def produceNode(s: String): NodeLike
}
```

Here's a [simple example](https://github.com/NicolasWinsten/web-viz/blob/master/src/main/scala/com/nick/webviz/example/TrivialExample.scala) of what that might look like.

After that, all you have to do is create a new WebView and pass in the `NodeFactory` you made:
```scala
import com.nick.webviz.WebView

object Main extends App {
  new WebView(YourNodeFactory, WINDOW_WIDTH, WINDOW_HEIGHT).main(args)
}
```

Run that Main, and type help in the text field of the Web window to take a look at how you can traverse your graph data.

If you have any questions, problems, or suggestions, please let me know by creating an issue. If there's something about my code you find abhorrent, please let me know that as well.  I'm new to Scala.
