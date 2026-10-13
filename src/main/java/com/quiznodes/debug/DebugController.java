package com.quiznodes.debug;

import java.util.HashMap;
import java.util.Map;

public class DebugController {
    private Map<String, DebugCommunication>  servers = new HashMap<>();

    public void addServer(String ip, int port){
        servers.put(ip + ":" + port,new DebugCommunication(ip,port));
    }
}
