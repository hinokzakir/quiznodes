# QuizNodes
Distributed file system for quizzes in JSON format with a client to server architecture. The goal is to distribute the files between servers and load balance clients between the servers to optimize performance. Each client will be able to select a JSON file entry and then get a local GUI to actually do the quiz based on the answers and questions inside of the JSON file.

## Setup
1. To install
```bash
cd quiznodes
mvn clean install

```

2. To compile
```bash
mvn clean package
```

3.1. To run main
```bash
mvn exec:java -Dexec.mainClass="com.quiznodes.Main"
```
or
```bash
mvn exec:java
```

3.2.
```bash
mvn exec:java -Dexec.mainClass="com.quiznodes.server.serverNode"
```



4. To run tests
```bash
mvn test
```