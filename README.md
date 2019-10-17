# MySlither
A Java implementation of the [slither.io](https://slither.io) client.

### Features / Screenshots
![Screenshot01](https://cloud.githubusercontent.com/assets/11258252/15582289/741d9dbe-2370-11e6-82a8-2dc135f823b6.png)

### Future Features
- correct snake-length
- correct snake-thickness
- respect lag_mult
- slithering
- bot-layer

### Prerequisites

- Java 8 (Darcula causes problems with Java 9+, see https://github.com/bulenkov/Darcula/issues/41)
- Gradle 5.3 or later

### Import into IntelliJ IDEA / Usage

- Open the Gradle project
- Enable "Use auto-import"
- Select "Use local gradle distribution"

Build the jar with `gradle clean shadowJar` and run that jar, or use `gradle run`.

### License
This project is released under the GNU/GPLv3 License. See [LICENSE](LICENSE) for details.
