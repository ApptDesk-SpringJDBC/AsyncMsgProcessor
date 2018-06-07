package com.ca.asynchmsg.store.iso;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Logger;
import com.ca.asynchmsg.message.Message;
import com.ca.asynchmsg.message.MessageUID;
import com.ca.asynchmsg.store.MessageStorage;
/**
 * 
 * @author upara01
 *ISORespMessageStore : This is the Response message store class and used to create and manage response Store.
 *It is an implementation of {@link MessageStorage}.
 */
public class ISORespMessageStore implements MessageStorage {
	private Map<String,Message> isoRespStoreMap;
	private Logger logger;
	
	/**
	 * ISORespMessageStore(Logger) is a parameterized constructor
	 * @param logger
	 */
	public ISORespMessageStore(Logger logger){
		isoRespStoreMap = new ConcurrentHashMap<String, Message>();
		this.logger = logger;
	}
	/**
	 * storeMessage(Message) method stores message in ResponseStore.
	 * @param msg : msgUID is the MessageUID entry of the Message object.
	 * @return Returns size of the response store (isoRespStoreMap).
	 */
	public int storeMessage(Message msg) {
		MessageUID messageUID = msg.getMessageUID();
		
		isoRespStoreMap.put(messageUID.getUID(), msg);
		if (logger.isTraceEnabled()){
			logger.trace("( respStore, storeMessage( "+messageUID.getMaskedUID()+" ) ) Store Size [ "+isoRespStoreMap.size()+" ] Message [ "+msg.getMessageContents()+" ]");
		}else if (logger.isDebugEnabled()){
			logger.debug("( respStore, storeMessage( "+messageUID.getMaskedUID()+" ) ) Store Size [ "+isoRespStoreMap.size()+" ]");
		}else{}
		return isoRespStoreMap.size();
	}
	
	/**
	 * retreiveMessage(MessageUID) method retrieves the Message from the response Store.
	 * @param msgUID : msgUID is the MessageUID entry of the Message object.
	 * @return Returns the message found in the response store.
	 */
	public Message retreiveMessage(MessageUID msgUID) {
		
		Message respMessage = null;
		if(isoRespStoreMap!=null && isoRespStoreMap.containsKey(msgUID.getUID())){
			respMessage = isoRespStoreMap.get(msgUID.getUID());
			isoRespStoreMap.remove(msgUID.getUID());
			MessageUID respMsgUID = respMessage.getMessageUID();
			if (logger.isTraceEnabled()){
				logger.trace("( respStore, retreiveMessage( "+respMsgUID.getMaskedUID()+" ) ) Store Size [ "+isoRespStoreMap.size()+" ] Message [ "+respMessage.getMessageContents()+" ]");
			}else if(logger.isDebugEnabled()){
				logger.debug("( respStore, retreiveMessage( "+respMsgUID.getMaskedUID()+" ) ) Store Size [ "+isoRespStoreMap.size()+" ]");
			}else{}
		}else{
			if (logger.isDebugEnabled()){
				logger.debug("( respStore, retreiveMessage( "+msgUID.getMaskedUID()+" ) ) Message Not Found....");
			}
		}
		return respMessage;
	}
	/**
	 * deleteMessage(Message) deletes message from response Store.
	 * @param msg : Message will be deleted from response store.
	 * @return Returns true in case message deleted successfully other wise false
	 */
	public boolean deleteMessage(Message msg) {
		MessageUID messageUID = msg.getMessageUID();
		
		if (isoRespStoreMap.containsKey(messageUID.getUID())){
			isoRespStoreMap.remove(messageUID.getUID());
			if (logger.isTraceEnabled()){
				logger.trace("( respStore, deleteMessage( "+messageUID.getMaskedUID()+" ) ) Store Size [ "+isoRespStoreMap.size()+" ] Message [ "+msg.getMessageContents()+" ]");
			}else if(logger.isDebugEnabled()){
				logger.debug("( respStore, deleteMessage( "+messageUID.getMaskedUID()+" ) ) Store Size [ "+isoRespStoreMap.size()+" ]");
			}else{}
			return true;
		}else{
			logger.warn("( respStore, deleteMessage( "+messageUID.getMaskedUID()+" ) ) Message Not Found....");
			return false;
		}
	}
}
