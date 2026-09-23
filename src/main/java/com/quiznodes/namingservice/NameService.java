package com.quiznodes.namingservice;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface NameService extends Remote {
    void registerNode(String name, String ip, int port) throws RemoteException;
    void unregisterNode(String name) throws RemoteException;
    // function to get all nodes connected to nameservice
    Map<String, Map<String, Integer>> resolveGroup() throws RemoteException;
}