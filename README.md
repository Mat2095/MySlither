# MySlither
A Java implementation of the slither.io client.

## Features / Screenshots
![Screenshot01](https://cloud.githubusercontent.com/assets/11258252/15582289/741d9dbe-2370-11e6-82a8-2dc135f823b6.png)

## Future Features
- correct snake-length
- correct snake-thickness
- respect lag_mult
- slithering
- bot-layer

## Libraries (not included)
- [Java-WebSocket](https://github.com/TooTallNate/Java-WebSocket), a Websocket implementation for Java
- [Darcula](https://github.com/bulenkov/Darcula), a Darcula-Look-and-Feel for Java (optional, but I'd recommend using it)

## Installation
I'm using NetBeans, the steps for Eclipse or IntelliJ IDEA might vary a bit.
- Get the sourcecode, e.g. via `git clone` or just download the ZIP-folder from GitHub and extract it
- In your IDE, create a new Java-Project with existing source
- Download the Java-WebSocket-Library (see link above, go to releases, /dist/java_websocket.jar) and add it to the project in your IDE
- Either download the Darcula-Library and add it as well, or remove the following line in Main.java:
  ```
  UIManager.setLookAndFeel("com.bulenkov.darcula.DarculaLaf");
  ```
- Set the main-class to `de.mat2095.my_slither.Main` in your IDE
- Run the project

## License
This project is released under the GNU/GPLv3 License. See [LICENSE](LICENSE) for details.
