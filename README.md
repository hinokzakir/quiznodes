# QuizNodes
Distributed file system for quizzes in JSON format with a client to server architecture. The goal is to distribute the files between servers and load balance clients between the servers to optimize performance. Each client will be able to select a JSON file entry and then get a local GUI to actually do the quiz based on the answers and questions inside of the JSON file.

## Used ports:
| Service        | Port   |
|----------------|--------|
| Naming Service | 1099   |
| Leader Info    | 1337   |
| Debugger       | 1112   |
| Server Node    | Random |
| Client Node    | Random |

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

3. 
If the you want to debug the cluster, the debugger has to run before the cluster has started. To run the debugger:
```bash
java -cp target/quiznodes-1.0.jar com.quiznodes.debug.DebugServer
```

4. The cluster needs both naming service and leader info service before cluster has started.

To run the naming service:
```bash
java -cp target/quiznodes-1.0.jar com.quiznodes.namingservice.Server
```

To run the Leader info:
```bash
java -cp target/quiznodes-1.0.jar com.quiznodes.info.LeaderInfoServer
```


5. To run the serverApp:
```bash
java -cp target/quiznodes-1.0.jar com.quiznodes.ServerApp
```
6. To run Client node (GUI)
```bash
java -cp target/quiznodes-1.0.jar com.quiznodes.App
```