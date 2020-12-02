# web-viz
An interactive graph visualizer!

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

