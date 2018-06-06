/**
 * 
 */
package com.ca.asynchmsg.workers;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;

import org.apache.log4j.Logger;

import com.ca.asynchmsg.beans.ConnectionBean;
import com.ca.asynchmsg.connectionset.ConnectionPool;
import com.ca.asynchmsg.message.Message;
import com.ca.asynchmsg.message.MessageUID;
import com.ca.asynchmsg.store.MessageStorage;

/**
 * @author upara01
 * MsgSenderThread will keep looking up ReqMsgQueue for messages and consequently write the message to the sockets stream.
 */
public class MessageSenderThread extends Thread{
	
	private ConnectionBean connBean;
	private MessageStorage reqStore;
	private Logger logger;
	private boolean stopRequested = false;
	
	/**
	 * MessageSenderThread(): A parameterized constructor
	 * @param connBean : ConnectionBean object that holds connection related parameters
	 * @param isoReqStore : {@link MessageStorage} object i.e Request Message Store
	 * @param logger : the Logger for a specific connection.
	 */
	public MessageSenderThread(ConnectionBean connBean, MessageStorage isoReqStore, Logger logger){
		this.connBean = connBean;
		this.reqStore = isoReqStore;
		this.logger = logger;
	}

	@Override
	public void run() {
		//blank message UID indicates that we just need to peek a Message from the queue 
		logger.info("started MessageSenderThread....");
		while(!stopRequested) {
			MessageUID messageUID = new MessageUID();
			messageUID.setMaskedUID("");
			messageUID.setUID("");
			Message msg = reqStore.retreiveMessage(messageUID);
			if(msg!=null){
				writeData(msg);
			}
		}
	}
	
	private void writeData(Message msg){
    	OutputStream os = null;
		try {
			os = connBean.getSocket().getOutputStream();
			String request = msg.getMessageContents();
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
				logger.trace(" writeData( " + byteBuffer.length + " , " + msg.getMessageUID().getMaskedUID() + " ) Data To be Sent [ " + request + " ]");
			}else{
				logger.info(" writeData( " + byteBuffer.length + " , " + msg.getMessageUID().getMaskedUID() + " )  Data To be Sent");
			}
			os.write(byteBuffer, 0, byteBuffer.length);
			os.flush();
		} catch (SocketException e) {
			logger.error(" writeData():SocketException:: ", e);
    		try {    			
    			if(connBean !=null && ConnectionPool.getHandleToContainer(connBean.getUniqueName()) !=null)
    			{
    				if(!stopRequested)
    					ConnectionPool.getHandleToContainer(connBean.getUniqueName()).reConnect(connBean.getUniqueName());
				}    			
			} catch (Exception e1) {
				logger.error(" writeData():Exception:: ", e1);
			}
    	} catch (IOException e) {
			logger.error(" writeData( " + msg.getMessageUID().getMaskedUID() + " ) IOException ",e);
		}			
    }
	/**
	 * requestStop() : Stops the MessageSenderThread{@link MessageSenderThread}
	 */
	public void requestStop() {
		logger.info("MessageSenderThread is going to stop....");
		stopRequested = true;
	}

}
