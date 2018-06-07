package com.ca.asynchmsg.container;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jpos.iso.ISODate;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.ISO87APackager;

import com.ca.asynchmsg.beans.ConnectionBean;
import com.ca.asynchmsg.connectionset.ConnectionPool;
import com.ca.asynchmsg.initializer.ATMSwitchTCPHandler;
import com.ca.asynchmsg.logger.AsynchLogger;
import com.ca.asynchmsg.logger.LoggerPool;
import com.ca.asynchmsg.message.MessageUIDGenerator;
import com.ca.asynchmsg.messageprocessor.MessageProcessor;
import com.ca.asynchmsg.messageprocessor.iso.ISOMessageProcessor;
import com.ca.asynchmsg.store.MessageStorage;
import com.ca.asynchmsg.store.iso.ISOReqMessageStore;
import com.ca.asynchmsg.store.iso.ISORespMessageStore;
import com.ca.asynchmsg.workers.MessageReceiverThread;
import com.ca.asynchmsg.workers.MessageSenderThread;
import com.ca.asynchmsg.workers.NetworkMessageThread;

/**
 * 
 * @author upara01
 * ServerContainer : This class will hold all the information about server(back end) for processing the request.
 */
public class ServerContainer{
	
	private ConnectionBean connBean;
	private MessageStorage isoReqStore;
	private MessageStorage isoRespStore;
	private MessageProcessor messageProcessor;
	private Logger logger;
	private MessageUIDGenerator messageUIDGenerator;
	private MessageSenderThread senderThread;
	private MessageReceiverThread receiverThread;
	private Thread networkThread;
	private Map<String, Object> lockMap = new ConcurrentHashMap<String,Object>();
	
	public ServerContainer(ConnectionBean connBean, String logFileLocation, Level level, MessageUIDGenerator messageGenerator) throws Exception{
		this.connBean = connBean;
		Logger asynchLogger = null;
		if(LoggerPool.getHandleToLogger(connBean.getUniqueName()) ==null){
			asynchLogger = (new AsynchLogger(logFileLocation+"/"+connBean.getUniqueName(), level)).getLogger(connBean.getUniqueName());
			LoggerPool.addLogger(connBean.getUniqueName(), asynchLogger);
		}else{
			asynchLogger = (new AsynchLogger(logFileLocation+"/"+connBean.getUniqueName(), level)).setLoggerProperties(LoggerPool.getHandleToLogger(connBean.getUniqueName()),level);
			LoggerPool.addLogger(connBean.getUniqueName(), asynchLogger);
		}
		this.logger = LoggerPool.getHandleToLogger(connBean.getUniqueName());
		this.messageUIDGenerator = messageGenerator;
	}
	
	public ConnectionBean getConnectionHandle(){
		return connBean;
	}
	
	public Logger getContainerLogger(){
		return logger;
	}
	
	public MessageProcessor getMessageProcessor(){
		return messageProcessor;
	}
	
	public MessageStorage getReqStore(){
		return isoReqStore;
	}
	
	public MessageStorage getRespStore(){
		return isoRespStore;
	}
	
	public MessageUIDGenerator getMessageUIDGenerator() {
		return messageUIDGenerator;
	}

	/**
	 * connect(boolean) : It connects socket to its back end.
	 * @param reconnect : It States whether connection to be established is fresh connect or reconnect.
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	public boolean connect(boolean reconnect) throws IOException, InterruptedException, ExecutionException{
		boolean isConnectSuccessful = false;
		SocketAddress sa = new InetSocketAddress(connBean.getHost(),connBean.getPort());
		Socket socket = new Socket();		
		for(int i=0;i<connBean.getRetry();i++){
			try {
				socket.connect(sa, connBean.getConnTimeOut());
				socket.setSoTimeout(connBean.getReadTimeOut());
				break;
			}catch(Exception e){
				logger.error("( connect() ) Issue connecting [ " + (i+1) + "] Time ", e);
				continue;
			}
		}
			
		if (socket.isConnected()){
			connBean.setSocket(socket);
			logger.info("( connect() ) Connection is established....");
			if(reconnect){
				logger.warn("( connect() ) Recycling the Connection....");
				ConnectionPool.recycleConnection(connBean.getUniqueName(), this);
				isConnectSuccessful = true;
			}else{ 
				isConnectSuccessful = startThreads();
			}
		}else{
			if (socket!=null){
				socket.close();
			}
		}
		return isConnectSuccessful;
	}
	
	/**
	 * reConnect() This Method is for reconnecting to the back end.
	 * @param connId : The connection Id for which reconnect will happen
	 * @return Returns true in case of success else false
	 */	
	public boolean reConnect(String connId) throws Exception {
		logger.warn(" ( reConnect() ) Socket is disconnected ... Trying to connect again......");
		
		try {			
			this.receiverThread.requestStop();
			this.receiverThread.interrupt();			
			if(connect(true)){
				startEchoHandler(true);
			}else{
				cleanResource(true);
				ConnectionPool.removeConnection(connBean.getUniqueName());
			}
			if(ATMSwitchTCPHandler.manualStopPool.contains(connId)){
				ATMSwitchTCPHandler.manualStopPool.remove(connId);
			}
			ATMSwitchTCPHandler.manualStopPool.put(connId, Boolean.FALSE);
			return true;
		} catch (Exception e) {
			logger.error(" ( reConnect() ) Exception ",e);
			return false;
		}
	}
	/**
	 * startThreads() method starts messageSender and messageReceiver Threads
	 */
	private boolean startThreads(){
		boolean isThreadStarted = false;
		isoReqStore = new ISOReqMessageStore(logger);
		isoRespStore = new ISORespMessageStore(logger);
		messageProcessor = new ISOMessageProcessor(this, logger);
		
		startReqHandler();
		startRespHandler();
		
		ConnectionPool.addConnection(connBean.getUniqueName(), this);
		
		if (connBean.getEchoInterval() != 0){
			isThreadStarted = startEchoHandler(false);
		}
		return isThreadStarted;
	}
	
	private void startReqHandler(){
		this.senderThread = new MessageSenderThread(connBean, isoReqStore, logger);
		this.senderThread.setName("Sender-"+this.senderThread.getName());
		this.senderThread.start();
	}
	
	private void startRespHandler(){
		this.receiverThread = new MessageReceiverThread(connBean, isoRespStore, logger, messageUIDGenerator, lockMap);
		this.receiverThread.setName("Receiver-"+this.receiverThread.getName());
		this.receiverThread.start();
	}
	
	private boolean startEchoHandler(boolean reconnectFlag){
		boolean signonSuccessFlag = false;
		if(reconnectFlag){
			if (handleSignOnMsg()){
				logger.info("( Reconnect route, startEchoHandler() ) Sign On Successful....");
				signonSuccessFlag = true;
				startRespHandler();
			}else{
				logger.error("( Reconnect route, startEchoHandler() ) Sign On Failed....");
				logger.error("( Reconnect route, startEchoHandler() ) This Connection will no longer be used....");
				signonSuccessFlag = false;
				cleanResource(reconnectFlag);
				ConnectionPool.removeConnection(connBean.getUniqueName());
				}
		}
		else{
			this.networkThread = new NetworkMessageThread(connBean, logger);	
			if (((NetworkMessageThread) networkThread).handleNetworkManagementMsg(NetworkMessageThread.signOn)){
				logger.info("( startEchoHandler() ) Sign On Successful....");
				signonSuccessFlag = true;
				this.networkThread.setName("Network-"+this.networkThread.getName());
				this.networkThread.start();
			}else{
				logger.error("( startEchoHandler() ) Sign On Failed....");
				logger.error("( startThreads() ) This Connection will no longer be used....");
				signonSuccessFlag = false;
				cleanResource(reconnectFlag);
				ConnectionPool.removeConnection(connBean.getUniqueName());
			}			
		}
		return signonSuccessFlag;
	}
	
	private boolean handleSignOnMsg(){
		
		ISOMsg  req = new ISOMsg();
		req.setPackager (new ISO87APackager());

		ISOMsg res = new ISOMsg();
		res.setPackager(new ISO87APackager());
		Date d = new Date();
		String stan = "";
		try {
			req.setMTI("0800");
			req.set(7,ISODate.getDate(d)+ISODate.getTime(d));
			stan = generateRandom(6);
			req.set(11,stan);
			req.set(70,"001");
		} catch (ISOException e1) {
			logger.error(" ( Reconnect route, handleSignOnMsg() ) ISOException ",e1);
			return false;
		}

		String nwMessageResp = null;
		try {
			nwMessageResp = processReconnectRequest(new String(req.pack()), stan);
		} catch (Exception e) {
			logger.error(" ( Reconnect route, handleSignOnMsg() ) Exception ",e);
			return false;
		}
		if(nwMessageResp != null || !"".equals(nwMessageResp)){
			try {
				res.unpack(nwMessageResp.getBytes());
				if("0810".equals(res.getMTI()) && "00".equals(res.getValue(39)))
					return true;
			} catch (ISOException e) {
				logger.error(" ( Reconnect route, handleSignOnMsg() ) ISOException ",e);
			}
		}else{
			logger.info(" ( Reconnect route, handleSignOnMsg() ) No proper sign-on response received from iso backend; The response is either Null or Blank....");
		}
		return false;
	}
	
	private String generateRandom(int length) {
	    Random random = new Random();
	    char[] digits = new char[length];
	    digits[0] = (char) (random.nextInt(9) + '1');
	    for (int i = 1; i < length; i++) {
	        digits[i] = (char) (random.nextInt(10) + '0');
	    }
	    return new String(digits);
	}
		
	private synchronized String processReconnectRequest(String request, String nwMessage) {

		String resp = "";
		int respLen = 0;
		DataInputStream din = null;
		byte byteOne = 0;
    	byte byteTwo = 0;
			try {
				OutputStream os = connBean.getSocket().getOutputStream();
				InputStream in = connBean.getSocket().getInputStream();

				if (in.available() > 0) {
					try {
						byte[] discardByte = new byte[in.available()];
						respLen = in.read(discardByte);
					} catch (IOException e) {
						logger.info("[" + e.toString()+ "] occurred in Discarding Data for connID "
										+ connBean.getHost()
										+ ";"
										+ connBean.getPort() + "->" + e);
					} catch (Exception e) {
						logger.info("[" + e.toString()+ "] occurred in Discarding Data for connID "
										+ connBean.getHost()
										+ ";"
										+ connBean.getPort() + "->" + e);
					}

					logger.info("Discarded residue data of length : " + respLen);
				}

				byte[] byteBuffer = new byte[request.length() + 2];
				int length = request.length();
				if (request.length() < 256){
					byteBuffer[0] = (byte)0;
					byteBuffer[1] = (byte)length;
				}else{
					byteBuffer[0] = (byte)(length / 256);
					byteBuffer[1] = (byte)(length % 256);
				}
				byte[] reqBytes = request.getBytes();
				for (int j = 2; j < byteBuffer.length; j++) {
					byteBuffer[j] = reqBytes[j - 2];
				}
				if (logger.isTraceEnabled()){
					logger.trace(" ( Reconnect route, processReconnectRequest() ) Sign-on request Message : "+request);
				}
				os.write(byteBuffer, 0, byteBuffer.length);
				os.flush();
				
				din = new DataInputStream(in);
				byteOne = din.readByte();
				byteTwo = din.readByte();
				
			    int messageLength = (int)byteTwo;
			    if(messageLength < 0)
			    	 	messageLength = 256 + messageLength;
			    
			    byteBuffer = new byte[messageLength];
				din.read(byteBuffer);
				resp = new String(byteBuffer, 0, byteBuffer.length);	
				if (logger.isTraceEnabled()){
					logger.trace("processReconnectRequest( "+resp.length()+" ) Message Rcvd ( "+resp+" )");
					logger.trace("["+Thread.currentThread().getId()+", "+connBean.getUniqueName()+" ]"
							+ "RespRcvd - " + resp);
				}else{
					logger.info("processReconnectRequest( "+resp.length()+" ) Message Rcvd");
					logger.info("["+Thread.currentThread().getId()+", "+connBean.getUniqueName()+" ]"
							+ "RespRcvd");
				}
				
				

			} catch (IOException e) {
				//Catching an IO exception and calling re-initialize.
				logger.error("["+Thread.currentThread().getId()+", "+connBean.getUniqueName()+" ]"
						+ "[ " + e.toString()+ "] in Processing request for connID "
						+ connBean.getUniqueName()
						+ "->" + e);
				
			} catch (Exception e) {
				logger.error("["+Thread.currentThread().getId()+", "+connBean.getUniqueName()+" ]"
						+ "[" + e.toString()+ "] in Processing request for connID "
								+ connBean.getUniqueName()
								+ "->" + e);
			}
		return resp;
	}
	
	/**
	 * cleanResource() : Stops all threads and call closeResource()
	 * @return Returns true in case of success else false
	 */
	public boolean cleanResource(boolean reconnectFlag){
		if(!reconnectFlag){
			if (connBean != null
					&& ((NetworkMessageThread) this.networkThread)
							.handleNetworkManagementMsg(NetworkMessageThread.signOff)) {
				logger.info("( cleanResource() ) Sign off Successful....");
			} else {
				logger.error("( cleanResource() ) Sign off failed....");
			}
		}
		// Stopping Threads
		((NetworkMessageThread) this.networkThread).requestStop();
		this.networkThread.interrupt();
		
		this.senderThread.requestStop();
		this.senderThread.interrupt();
		
		this.receiverThread.requestStop();
		this.receiverThread.interrupt();
		
		this.closeResource();
		return true;		
	}
	
	/**
	 * closeResource() : Closes all input/output streams and socket as well.
	 */
	public void closeResource() {
		
		if (connBean.getSocket() != null) {

			OutputStream os = null;
			InputStream is = null;
			try {
				is = connBean.getSocket().getInputStream();
				os = connBean.getSocket().getOutputStream();
			} catch (IOException e1) {
				logger.error("( closeResource() ) IOException in fetching the streams", e1);
			} catch (Exception e1) {
				logger.error("( closeResource() ) Exception in fetching th streams", e1);
			}

			try {
				if (os != null)
					os.close();
			} catch (IOException e) {
				logger.error("( closeResource() ) IOException while closing output stream", e);
			}

			try {
				if (is != null)
					is.close();
			} catch (IOException e) {
				logger.error("( closeResource() ) IOException while closing input stream", e);
			}

			try {
				if (!connBean.getSocket().isClosed())
					connBean.getSocket().close();
			} catch (IOException e) {
				logger.error("( closeResource() ) IOException while closing socket", e);
			}
		}
	}

	public Map<String, Object> getLockMap() {
		return lockMap;
	}

	public void setLockMap(Map<String, Object> lockMap) {
		this.lockMap = lockMap;
	}
}
