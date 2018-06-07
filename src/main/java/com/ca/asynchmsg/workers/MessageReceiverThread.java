package com.ca.asynchmsg.workers;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

import com.ca.asynchmsg.beans.ConnectionBean;
import com.ca.asynchmsg.connectionset.ConnectionPool;
import com.ca.asynchmsg.iso.CAISO87APackager;
import com.ca.asynchmsg.message.Message;
import com.ca.asynchmsg.message.MessageUID;
import com.ca.asynchmsg.message.MessageUIDGenerator;
import com.ca.asynchmsg.store.MessageStorage;
import com.ca.asynchmsg.store.iso.ISORespMessageStore;


/**
 * @author upara01
 *
 * MsgRecieverThread will populate isoRespStoreMap on receiving a message on the stream from the backend.
 */
public class MessageReceiverThread extends Thread{

	private ConnectionBean connBean;
	private ISORespMessageStore respStore;
	private Logger logger;
	private boolean stopRequested = false;
	private MessageUIDGenerator messageUIDGenerator;
	private Map<String, Object> lockMap;

	/**
	 * MessageReceiverThread : A parameterized Constructor
	 * @param connBean : ConnectionBean object that holds connection related parameters
	 * @param isoRespStore : {@link MessageStorage} object i.e Response Message Store
	 * @param logger : the Logger for a specific connection.
	 * @param messageUIDGenerator : {@link MessageUIDGenerator}
	 */
	public MessageReceiverThread(ConnectionBean connBean, MessageStorage isoRespStore, Logger logger, MessageUIDGenerator messageUIDGenerator,Map<String, Object> lockMap){
		this.connBean = connBean;
		this.respStore = (ISORespMessageStore) isoRespStore;
		this.logger = logger;
		this.messageUIDGenerator = messageUIDGenerator;
		this.lockMap = lockMap;
	}

	@Override
	public void run() {
		logger.info("started MessageReceiverThread....");
		
		Message respMessage = null;
		DataInputStream din =null;
		Object lock = null; 
		try {
			din = new DataInputStream(connBean.getSocket().getInputStream());
			String response = null;
			ISOMsg res = new ISOMsg();
			CAISO87APackager caISO87Packager = new CAISO87APackager(messageUIDGenerator);
			res.setPackager(caISO87Packager);
			while (!stopRequested) {
				try {										
					response = receiveData(din);
					MessageUID messageUID = null;
					if (response != null && !"".equals(response)) {
						res.unpack(response.getBytes());
						if ("0810".equals(res.getMTI())) {
							String messageUIDStr = res.getString(11);
							messageUID = new MessageUID();	
							messageUID.setMaskedUID(messageUIDStr);
							messageUID.setUID(messageUIDStr);
						} else {
							messageUID = messageUIDGenerator.generateMessageUID(res);
						}
						respMessage = new Message(response, messageUID);
						respStore.storeMessage(respMessage);
						lock = lockMap.get(messageUID.getUID());
						if(lock != null) {
							synchronized (lock) {	
								lock.notify();
							}
						}else{
							logger.warn("The Message whose messageUID is [ "
								+ messageUID.getMaskedUID()
								+ " ] arrived late and will not be processed because it was already got timed out....");
						}
					}else{
						Thread.sleep(500);
					}
				} catch (ISOException e) {
					logger.error(" run() ISOException ", e);
				} catch (Exception e) {
					logger.error(" run() Exception ", e);
					continue;
				} 
			} 
		} catch (IOException e1) {
			logger.error(" MessageReceiverThread.run():IOException" +e1);
		}
	}
	
	/**
	 * will only return the message contents; not the first two bytes coming in from the server
	 * @return Returns the Response received by this function.
	 */
	private String receiveData(DataInputStream din){
    	String resp = null;
    	byte byteOne = 0;
    	byte byteTwo = 0;
    	try {
			if (din.available() > 0){
				 byteOne = din.readByte();
				 byteTwo = din.readByte();
			}else{
				 return null;
			}
		    int messageLength = (int)byteTwo;
		    if(messageLength < 0)
		    	 	messageLength = 256 + messageLength;
			
		    byte[] byteBuffer = new byte[messageLength];
			din.read(byteBuffer);
			resp = new String(byteBuffer, 0, byteBuffer.length);
			if (logger.isTraceEnabled()){
				logger.trace("receiveData( "+resp.length()+" ) Message Rcvd ( "+resp+" )");
			}else{
				logger.info("receiveData( "+resp.length()+" ) Message Rcvd");
			}
			return resp;    	
    	}catch (SocketException e) {
    		try {    			
    			if(connBean !=null && ConnectionPool.getHandleToContainer(connBean.getUniqueName()) !=null)
    			{
    				if(!stopRequested)
    					ConnectionPool.getHandleToContainer(connBean.getUniqueName()).reConnect(connBean.getUniqueName());
				}    			
			} catch (Exception e1) {
				logger.error(" receiveData():SocketException:: Exception ", e1);
			}
    	}catch (InterruptedIOException iioe) {
    		logger.warn("Remote host timed out during read operation");
    	}catch (IOException e) {
    		logger.error(" receiveData() IOException ", e);
		}
    	return null;
    }
	
	/**
	 * requestStop() : Stops the MessageReceiverThread{@link MessageReceiverThread}
	 */
	public void requestStop() {
		logger.info("MessageReceiverThread is going to stop....");
		stopRequested = true;
	}
}
