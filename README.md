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

3.1. To run Client node (GUI)
```bash
mvn exec:java
```

3.2. To run the serverApp
```bash
mvn exec:java -Dexec.mainClass="com.quiznodes.ServerApp"
```

3.3. To run the naming service
```bash
mvn exec:java -Dexec.mainClass="com.quiznodes.namingservice.Server"
```

3.4. To run the Leader info
```bash
mvn exec:java -Dexec.mainClass="com.quiznodes.info.LeaderInfoServer"
```



5. To run tests
```bash
mvn test
```