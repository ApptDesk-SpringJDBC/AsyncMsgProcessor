package com.ca.asynchmsg.initializer;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * 
 * @author upara01
 * ATMSwitchServletContextListener : A Context listener an implementation of ServletContextListener.
 */
@WebListener
public class ATMSwitchServletContextListener extends Thread implements ServletContextListener  {
	
	/**
	 * contextInitialized(ServletContextEvent) : Initialization code will be invoked by the ATMSwitchServletContextListener
	 * @param event : ServletContextEvent
	 */
	public void contextInitialized(ServletContextEvent event) {
		ServletContext context = event.getServletContext();
		ATMSwitchTCPHandler.initalizeConnections(context);
	}

	/**
	 * contextDestroyed(ServletContextEvent): cleanup all resources before tomcat shut-down
	 * @param event : ServletContextEvent
	 */
	public void contextDestroyed(ServletContextEvent event) {
		ATMSwitchTCPHandler.closeConnections();
	}

}
