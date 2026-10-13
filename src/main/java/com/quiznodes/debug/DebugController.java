package com.quiznodes.debug;

import com.quiznodes.proto.debugHeartbeatResponse;

import java.util.HashMap;
import java.util.Map;

public class DebugController {
    private Map<String, DebugCommunication>  servers = new HashMap<>();
    private Map<String, Long> capacities = new HashMap<>();
    private String leader;

    public void addServer(String ip, int port){
        servers.put(ip + ":" + port,new DebugCommunication(ip,port));
        System.out.println("addServer ip:"+ip+":"+port);
    }

    public void sendHeartbeat(){
        for (String id : servers.keySet()){
            DebugCommunication server =  servers.get(id);
            debugHeartbeatResponse response = server.sendHeartbeat();
            capacities.put(id, response.getCapacity());
            if(response.getLeader()){
                leader = id;
            }
        }
    }
}
