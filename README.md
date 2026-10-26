# QuizNodes

A distributed file system for quizzes in JSON format. Quizzes are replicated across server nodes with dynamic capacity management, and clients connect through a desktop GUI to browse, upload, and take quizzes.

## Ports

| Service | Port | Protocol | Description |
|---|---|---|---|
| Naming Service | `1099` | Java RMI | Node registry and cluster discovery |
| Leader Info | `1337` | gRPC | Tracks active leader node endpoint |
| Debugger | `1112` | gRPC | Cluster monitoring tool (optional) |
| Server Nodes | Dynamic | gRPC | Inter-server replication & client API |
| Client Nodes | Dynamic | gRPC | JavaFX desktop client |

## Build

Build the project and fat JAR:

```bash
mvn clean package
```

The executable JAR is created at `target/quiznodes-1.0.jar`.

## Running the System

Run each component in a separate terminal window, in the following order:

### 1. Core Services (Start first)

Start the Naming Service:
```bash
java -cp target/quiznodes-1.0.jar com.quiznodes.namingservice.Server
```

Start the Leader Info service:
```bash
java -cp target/quiznodes-1.0.jar com.quiznodes.info.LeaderInfoServer
```

### 2. Debugger (Optional)

If you want to monitor the cluster, launch the debugger before starting server nodes:
```bash
java -cp target/quiznodes-1.0.jar com.quiznodes.debug.DebugServer
```

### 3. Server Nodes

Start one or more server nodes. The first node to join becomes the leader, and subsequent nodes join as followers:
```bash
java -cp target/quiznodes-1.0.jar com.quiznodes.ServerApp
```

### 4. Client GUI

Launch the client application:
```bash
java -cp target/quiznodes-1.0.jar com.quiznodes.App
```