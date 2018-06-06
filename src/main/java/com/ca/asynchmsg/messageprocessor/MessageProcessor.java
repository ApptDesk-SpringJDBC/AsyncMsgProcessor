package com.ca.asynchmsg.messageprocessor;

import com.ca.asynchmsg.container.ServerContainer;
import com.ca.asynchmsg.message.Message;
import com.ca.asynchmsg.store.MessageStorage;

/**
 * 
 * @author sandipbose
 * 
 * This interface deals with processing of the message
 * 1. deliver message to a back end
 * 2. Store the message for future references
 * 3. Retrieve the response to the message from the back end 
 */
public interface MessageProcessor {
	public void deliverMessageToThirdParty(ServerContainer serverHandle,Message msg) throws Exception;
	public void persistMessage(MessageStorage store, Message msg) throws Exception;
	public Message retrieveMessageFromThirdParty(ServerContainer serverHandle,Message msg) throws Exception;
}
