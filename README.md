# cb-websocket

a lightweight remake of tellinq's cheatbreaker 1.7.10 websocket server, built with Java.

## Requirements

- java 8 or newer
- mongoDB instance

## Running

start your mongodb server, then run:

```bash
java -jar target/cb-websocket-server-1.0-SNAPSHOT.jar <port>
```

Example:

```bash
java -jar target/cb-websocket-server-1.0-SNAPSHOT.jar 8080
```

## Building

using maven:

```bash
mvn clean package
```

the compiled jar will be located at:

```
target/cb-websocket-server-1.0-SNAPSHOT.jar
```

## Project Status

this project is currently under development. apis and behavior may change as new features are implemented.

## Credits

- **tellinq**
- **jhalt**
- **and cheatbreaker team**

## Disclaimer

this project is an independent remake and is **not affiliated with or endorsed by CheatBreaker**.
