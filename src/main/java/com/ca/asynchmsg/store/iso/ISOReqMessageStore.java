package com.ca.asynchmsg.store.iso;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;

import com.ca.asynchmsg.message.Message;
import com.ca.asynchmsg.message.MessageUID;
import com.ca.asynchmsg.store.MessageStorage;

/**
 * 
 * @author upara01
 *ISOReqMessageStore : This is the Request message store class and used to create and manage request Store.
 *It is an implementation of {@link MessageStorage}.
 *
 */
public class ISOReqMessageStore implements MessageStorage {

	private Queue<Message> isoReqStore;
	private Logger logger;
	
	/**
	 * ISOReqMessageStore(Logger) is a parameterized constructor
	 * @param logger
	 */
	public ISOReqMessageStore(Logger logger){
		isoReqStore = new ConcurrentLinkedQueue<Message>();
		this.logger = logger;
	}
	
	/**
	 * storeMessage(Message) method stores message in RequestStore.
	 * @param msg : msgUID is the MessageUID entry of the Message object.
	 * @return Returns size of the request store (isoReqStore).
	 */
	public int storeMessage(Message msg) {
		MessageUID messageUID = msg.getMessageUID();
		//Added if condition to check whether same message already exist in the queue before adding msg to the queue.
		int size = 0;
		if (!isoReqStore.contains(msg)){
			isoReqStore.add(msg);
			size = isoReqStore.size();
			if (logger.isTraceEnabled()){
				logger.trace("( reqStore, storeMessage( "+messageUID.getMaskedUID()+" ) ) Store Size [ "+size+" ] Message [ "+msg.getMessageContents()+" ]");
			}else if (logger.isDebugEnabled()){
				logger.debug("( reqStore, storeMessage( "+messageUID.getMaskedUID()+" ) ) Store Size [ "+size+" ]");
			}else{}
		}else{
			size = isoReqStore.size();
			// sandy - need to handle this
		}
		return size;
	}

	/**
	 * retreiveMessage(MessageUID) method retrieves the Message from the request Store.
	 * @param messageUID : msgUID is the MessageUID entry of the Message object.
	 * @return Returns the message found in the request store by polling the isoReqStore
	 */
	public Message retreiveMessage(MessageUID messageUID) {
		
		Message msg = null;
		
		if (!isoReqStore.isEmpty()){
			msg = isoReqStore.poll();
			MessageUID msgUID = msg.getMessageUID();
			if (logger.isTraceEnabled()){
				logger.trace("( reqStore, retreiveMessage( "+msgUID.getMaskedUID()+" ) ) Store Size [ "+isoReqStore.size()+" ] Message [ "+msg.getMessageContents()+" ]");
			}else if(logger.isDebugEnabled()){
				logger.debug("( reqStore, retreiveMessage( "+msgUID.getMaskedUID()+" ) ) Store Size [ "+isoReqStore.size()+" ]");
			}else{}
			return msg;
		}else{
			return null;	
		}
	}

	/**
	 * deleteMessage(Message) deletes message from request Store.
	 * @param msg : Message will be deleted from response store.
	 * @return Returns true in case message deleted successfully other wise false
	 */
	public boolean deleteMessage(Message msg) {
		MessageUID messageUID = msg.getMessageUID();
		if (isoReqStore.contains(msg)){
			isoReqStore.remove(msg);
			if (logger.isTraceEnabled()){
				logger.trace("( reqStore, deleteMessage( "+messageUID.getMaskedUID()+" ) ) Store Size [ "+isoReqStore.size()+" ] Message [ "+msg.getMessageContents()+" ]");
			}else if(logger.isDebugEnabled()){
				logger.debug("( reqStore, deleteMessage( "+messageUID.getMaskedUID()+" ) ) Store Size [ "+isoReqStore.size()+" ]");
			}else{}
			return true;
		}else{
			logger.warn("( reqStore, deleteMessage( "+messageUID.getMaskedUID()+" ) ) Message Not Found....");
			return false;
		}
	}

}
