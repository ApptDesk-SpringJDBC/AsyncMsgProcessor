package com.ca.asynchmsg.logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

/**
 * 
 * @author upara01
 * 
 * This method deals with the life cycle of various Loggers
 * 1. Add Loggers to a Collection
 * 2. Get a Logger object From the Collection by passing connection Name.
 * 3. Remove a Logger from the Collection
 * 
 */
public class LoggerPool{
	
	private static Map<String, Logger> LOGGER_MAP = 
			new ConcurrentHashMap<String, Logger>();
	/**
	 * Default constructor
	 */
	public LoggerPool(){}
	
	/**
	 * addLogger(String,Logger) : It adds a new logger in the logger Poll if already exists then replaces with new one.
	 * @param connName : The connection name for which logger will be added in the pool.
	 * @param asynchLogger : Logger to be added in the pool
	 */
	public static void addLogger(String connName, Logger asynchLogger){
		if (LOGGER_MAP.containsKey(connName)){
			LOGGER_MAP.replace(connName, asynchLogger);
		}else{
			LOGGER_MAP.put(connName, asynchLogger);
		}
	}
	
	/**
	 * removeLogger(String) : Removes logger from the poll for a particular connection name.
	 * @param connName : The connection name against logger will be removed from the pool.
	 */
	public static void removeLogger(String connName){
		if (LOGGER_MAP.containsKey(connName)){
			LOGGER_MAP.remove(connName);
		}
	}
	
	/**
	 * getHandleToLogger(String) : Provides The logger from the pool against connection name.
	 * @param connName : The connection name to be provided to get logger from the pool.
	 * @return : Returns Logger
	 * @throws Exception
	 */
	public static Logger getHandleToLogger(String connName) throws Exception{
		if (LOGGER_MAP.containsKey(connName)){
			return (Logger)LOGGER_MAP.get(connName);
		}else{
			return null;
		}
	}

}
