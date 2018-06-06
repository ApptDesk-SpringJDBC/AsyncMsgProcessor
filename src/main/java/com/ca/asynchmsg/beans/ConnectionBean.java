package com.ca.asynchmsg.beans;

import java.net.Socket;

/**
 * 
 * @author upara01
 * ConnectionBean : It holds connection related variables like host,port,connection timeout,retry, e.t.c. and its getters and setters.
 */
public class ConnectionBean {
	
	/**
	 * A parameterized constructor
	 * @param host : IP of a back end
	 * @param port : port number of a back end
	 * @param connTimeOut : back end connection timeout
	 * @param readTimeOut : socket read timeout
	 * @param threadTimeOut : request timeout
	 * @param retryCount : The number of connect retry.
	 * @param echoInterval : The interval at which echo test will be sent.
	 */
	public ConnectionBean(String host, int port, 
			int connTimeOut, int readTimeOut, int threadTimeOut,
			int retryCount, int echoInterval){
		this.host = host;
		this.port = port;
		this.connTimeOut = connTimeOut;
		this.readTimeOut = readTimeOut;
		this.threadTimeOut = threadTimeOut;
		this.retryCount = retryCount;
		this.echoInterval = echoInterval;
		this.uniqueName = host+"_"+port;
	}
	public Socket getSocket() {
		return socket;
	}
	public void setSocket(Socket socket) {
		this.socket = socket;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public int getConnTimeOut() {
		return connTimeOut;
	}
	public void setConnTimeOut(int connTimeOut) {
		this.connTimeOut = connTimeOut;
	}
	public int getReadTimeOut() {
		return readTimeOut;
	}
	public void setReadTimeOut(int readTimeOut) {
		this.readTimeOut = readTimeOut;
	}
	public int getRetry() {
		return retryCount;
	}
	public void setRetry(int retryCount) {
		this.retryCount = retryCount;
	}
	public String getUniqueName() {
		return uniqueName;
	}
	public void setUniqueName(String uniqueName) {
		this.uniqueName = uniqueName;
	}
	public int getEchoInterval() {
		return echoInterval;
	}
	public void setEchoInterval(int echoInterval) {
		this.echoInterval = echoInterval;
	}
	public int getThreadTimeOut() {
		return threadTimeOut;
	}
	public void setThreadTimeOut(int threadTimeOut) {
		this.threadTimeOut = threadTimeOut;
	}
	
	@Override
    public String toString() {
    	if (this.echoInterval == 0){
    		return "[ ("+uniqueName+") : "+host+","+port+","+connTimeOut+","+readTimeOut+","+retryCount+"]";
    	}else{
    		return "[ ("+uniqueName+") : "+host+","+port+","+connTimeOut+","+readTimeOut+","+retryCount+","+echoInterval+"]";
    	}
    }
	
	private Socket socket = null;
	private String host;
	private int port = 0;
	private int connTimeOut = 0;
	private int readTimeOut = 0;
	private int retryCount = 0;
	private String uniqueName;
	// 0 - denotes that that there is no ECHO setup
	private int echoInterval = 0;
	private int threadTimeOut = 0;
	
}
