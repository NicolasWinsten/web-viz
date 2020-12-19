# web-viz
An interactive graph visualizer!
![Kevin Bacon](/KevinBaconViz.PNG)
![Spider web](/SpiderWebViz.PNG)
![Numbers](/NumbersViz.PNG)

web-viz is a tool that can allow you to interactively traverse whatever graph data you want!
The above two examples is what you get when you use Wikipedia articles and categories as your graph data.
Left/Right clicking a title will produce more nodes in the web that you can also click on.

# How to use:
If you just want to play around with the wiki-web example shown above, clone the repository and run 'Main' in 'com.nicolaswinsten.webviz.wikiweb'. Right click the canvas, and enter a Wikipedia title to get started. Also, double mouse clicking on a node will open its wikipedia webpage, so you can read about it!

If you want to make a web using your own graph data, do the following:

Add this to your `build.sbt`:
```scala
// Scala Swing required for formatting your Nodes with Color and Font
libraryDependencies += "org.scala-lang.modules" %% "scala-swing" % "3.0.0"

externalResolvers += "web-viz packages" at "https://maven.pkg.github.com/NicolasWinsten/web-viz"
libraryDependencies += "com.nicolaswinsten" %% "web-viz" % "1.2.0"
```

Next, you'll want to define your own node class extending `NodeLike`:
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
```

Here's a [simple example](https://github.com/NicolasWinsten/web-viz/blob/master/src/main/scala/com/nick/webviz/example/TrivialExample.scala) of what that might look like.

After that, all you have to do is create a new `WebView` and pass in a `String => YourNodeLike` function that produces one of your `NodeLike` objects from a String:
```scala

object Main extends App {
  new WebView(stringToNodeFunc, WINDOW_WIDTH, WINDOW_HEIGHT).main(args)
}
```
The `stringToNodeFunc` function is so that you can add nodes via a text field in the view.

Run that Main, and right click the canvas to add your first node.

## Placing a Web into your own Swing app:
If you would like to embed a Web view into your own Swing app, you will have to repeat all the steps outlined above, but instead of running `WebView`, you can instantiate a `WebCanvas` which is a Panel that you can insert into your own Swing app:
```scala
import com.nicolaswinsten.webviz.WebCanvas

object YourSwingApp extends SimpleSwingApplication {
  override def top: Frame = new MainFrame {
    
    contents = new WebCanvas(stringToNodeFunc) {
      preferredSize = new Dimension(WIDTH, HEIGHT)
    }
    
  }
}
```

If you have any questions, problems, or suggestions, please let me know by creating an issue. If there's something about my code you find abhorrent, please let me know that as well.  I'm new to Scala.
