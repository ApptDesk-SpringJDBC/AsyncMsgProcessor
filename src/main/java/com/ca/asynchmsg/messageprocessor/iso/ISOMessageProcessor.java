package com.ca.asynchmsg.messageprocessor.iso;

import javax.naming.TimeLimitExceededException;

import org.apache.log4j.Logger;

import com.ca.asynchmsg.container.ServerContainer;
import com.ca.asynchmsg.initializer.ATMSwitchTCPHandler;
import com.ca.asynchmsg.message.Message;
import com.ca.asynchmsg.messageprocessor.MessageProcessor;
import com.ca.asynchmsg.store.MessageStorage;
import com.ca.asynchmsg.store.iso.ISOReqMessageStore;
import com.ca.asynchmsg.store.iso.ISORespMessageStore;
/**
 * @author upara01
 * ISOMessageProcessor is a message processor which invokes below methods.
 * 1. deliverMessageToThirdParty() : This will store the message to the requestStore Or responseStore
 * 2. retrieveMessageFromThirdParty() : This Method will be invoked by Tomcat Thread to retrieve message from ResponseStore.
 */

public class ISOMessageProcessor implements MessageProcessor {

	private ServerContainer serverHandle;
	private Logger logger;
	
	public ISOMessageProcessor(ServerContainer serverHandle, Logger logger){
		this.serverHandle = serverHandle;
		this.logger = logger;
	}
	
	/**
	 * deliverMessageToThirdParty(ServerContainer, Message) : Persist the message object by calling persistMessage().
	 * @param serverHandle : The serverContainer object of a specific connection
	 */
	public void deliverMessageToThirdParty(ServerContainer serverHandle, Message msg) throws Exception{
		persistMessage(serverHandle.getReqStore(), msg);

	}
	
	/**
	 * persistMessage(MessageStorage,Message) : It stores the message either to the request Store or response store runtime.
	 * @param store : The MessageStorage object either {@link ISORespMessageStore} or {@link ISOReqMessageStore}
	 * @param msg : Message object which needs to be stored.
	 */
	public void persistMessage(MessageStorage store, Message msg) throws Exception{
		store.storeMessage(msg);
	}
	
	/**
	 * retrieveMessageFromThirdParty(ServerContainer): Retrieve message from ResponseStore.
	 * @param serverHandle : The serverContainer object of a specific connection
	 * @return Returns the response of a request.
	 */
	public Message retrieveMessageFromThirdParty(ServerContainer serverHandle, Message msg) throws Exception{
		long now=System.currentTimeMillis();
		int timeout = serverHandle.getConnectionHandle().getThreadTimeOut();
		Message respMessage = null;
		ISORespMessageStore isoRespStore = (ISORespMessageStore) serverHandle.getRespStore();
		try{	
			while(true){
				respMessage = isoRespStore.retreiveMessage(msg.getMessageUID());
				if(respMessage ==null){
					if(now+timeout <= System.currentTimeMillis()){
						throw new TimeLimitExceededException();
					}else{
						Thread.sleep(timeout/10);
						continue;
					}
				}else{
					break;
				}
			}	
		}catch(TimeLimitExceededException e){
			logger.warn("Client thread got Timed out...............");
			if(!isoRespStore.deleteMessage(msg)){
				int setSize = isoRespStore.addToUnprocessedSet(msg.getMessageUID().getUID());
				if(setSize>0)
					ATMSwitchTCPHandler.logger.info("UnprocessedSet has "+setSize+" number of elements....");
				logger.warn("( unProcessedMsgSet, addToUnprocessedSet( "+msg.getMessageUID().getMaskedUID()+" ) ) Store Size [ "+setSize+" ] Response Not Recieved....");
			}
		}
		return respMessage;
	}

}
