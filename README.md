# TetromiNET

Browser-based, multiplayer, open-source implementation of the popular tetromino puzzle game.

## Features

- Written entirely in Scala, a functional/object-oriented programming language that compiles to Java bytecode and is fully interoperable with existing Java libraries.
- Model-View-Controller design pattern effectively separates the game logic, the server framework, and the front-end Java applet modules. A fully-functional text-mode interface is also included to demonstrate the model-view separation.
- Extremely efficient XML-based client-server protocol designed to cope with arbitrary latency between multiple opponents, and server-side game logic completely eliminates any possibility of cheating or hacked clients. Clients capture and send compressed sequences of controller inputs, and the game is replayed exactly on both the opponent's client and the delegating server.
- Uses the Lift web framework contained in a Tomcat instance to implement the HTTP game server, and manages browser session state in order to match games to users.

## Dependencies

- [Maven](http://maven.apache.org/) (project dependency management)
- [Scala](http://www.scala-lang.org/) (Programming language compiler and libraries)
- [Lift](http://liftweb.net) (Scala-based web framework)
- [PulpCore](https://code.google.com/p/pulpcore/) (2D Java applet framework)
- [Charva](http://www.pitman.co.za/projects/charva/index.html) (Text-mode API bindings for Java)

## Install

Compilation is done through the Maven framework.

## License

[GNU General Public License](http://www.gnu.org/licenses/) Version 3 or later.

## Authors

Will Jordan (will.jordan@gmail.com)

## Contact

Will Jordan (will.jordan@gmail.com)
