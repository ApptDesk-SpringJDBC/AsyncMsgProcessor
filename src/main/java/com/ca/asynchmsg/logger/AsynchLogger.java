package com.ca.asynchmsg.logger;


import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 * 
 * @author upara01
 * AsynchLogger is used to create different logger for different connection.
 */
public class AsynchLogger {
	
	/**
	 * AsynchLogger(String,Level) : Parameterized Constructor
	 * @param logName : The Actual file path and log file name
	 * @param level : Level of logging
	 */
	public AsynchLogger(String logName, Level level){
		 this.logName = logName;
		 this.level = level;
	}
	
	/**
	 * getLogger(String) : Logger provider
	 * @param category : Logger object will be provided based on the category
	 * @return Returns a Logger object
	 */
	public Logger getLogger(String category) {
		Logger logger = Logger.getLogger(category);
		DailyRollingFileAppender fa = new DailyRollingFileAppender();
		fa.setFile(this.logName);
		fa.setLayout(new PatternLayout("%d | %-5p | [%t:%c{1}] | %m%n"));
		fa.setDatePattern("'.'yyyy-MM-dd");
		fa.setAppend(true);
		fa.activateOptions();
		logger.addAppender(fa);
		logger.setLevel(this.level);
		return logger;
	}
	
	/**
	 * setLoggerProperties(Logger,Level): It changes the logging properties of existing logger 
	 * @param logger : The existing logger object for which logging properties to be changed
	 * @param level : The new logging Level
	 * @return Returns Logger Object after changing its properties
	 */
	public Logger setLoggerProperties(Logger logger, Level level){
		logger.setAdditivity(false);
		logger.setLevel(level);
		return logger;		
	}

	public String getLogName() {
		return logName;
	}

	public void setLogName(String logName) {
		this.logName = logName;
	}

	public Level getLevel() {
		return level;
	}

	public void setLevel(Level level) {
		this.level = level;
	}

	private String logName;
	private Level level;

}
