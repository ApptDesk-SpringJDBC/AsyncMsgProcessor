package com.ca.asynchmsg.connectionset;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ca.asynchmsg.container.ServerContainer;

/**
 * 
 * @author sandipbose
 * 
 * This method deals with the life cycle of various back ends
 * 1. Add Back ends to a Collection
 * 2. Recycle a bank ends in case there is an intermittent issue with the back end
 * 3. Remove a back end from the Collection
 * 4. Get a handle to the back end for further processing of messages 
 * 
 */
public class ConnectionPool{
	
	private static Map<String, ServerContainer> CONN_MAP = 
			new ConcurrentHashMap<String, ServerContainer>();
	
	/**
	 * ConnectionPool() : Default constructor
	 */
	public ConnectionPool(){}
	
	/**
	 * addConnection(String,ServerContainer): Adds serverContainer to the connection Pool against a connection id.
	 * @param connName : The connection Id of the back end
	 * @param serverContainer : The serverContainer object of a specific connection
	 */
	public static void addConnection(String connName, ServerContainer serverContainer){
		if (CONN_MAP.containsKey(connName)){
			recycleConnection(connName, serverContainer);
		}else{
			CONN_MAP.put(connName, serverContainer);
		}
	}
	
	/**
	 * recycleConnection(String,ServerContainer) : It will replace the existing serverContainer in the pool for a connection id.
	 * @param connName : The connection Id of the back end
	 * @param serverContainer : The serverContainer object of a specific connection
	 */
	public static void recycleConnection(String connName,ServerContainer serverContainer){
		CONN_MAP.replace(connName, serverContainer);
	}
	

	/**
	 * removeConnection(String) : It will remove the existing connection from the pool.
	 * @param connName : The connection Id of the back end
	 */
	public static void removeConnection(String connName){
		if (CONN_MAP.containsKey(connName)){
			CONN_MAP.remove(connName);
		}else{
			
		}
	}
	
	/**
	 * getHandleToContainer(String) : Finds the ServerContainer from the connection pool against a connection id.
	 * @param connName
	 * @return Returns a serverContainer object for a connection ID.
	 * @throws Exception
	 */
	public static ServerContainer getHandleToContainer(String connName) throws Exception{
		if (CONN_MAP.containsKey(connName)){
			return (ServerContainer)CONN_MAP.get(connName);
		}else{
			return null;
		}
	}
	
	public static Map<String, ServerContainer> getConnectionMap(){
		return CONN_MAP;		
	}
	
}
