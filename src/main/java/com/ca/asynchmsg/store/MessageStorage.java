package com.ca.asynchmsg.store;

import com.ca.asynchmsg.message.Message;
import com.ca.asynchmsg.message.MessageUID;

/**
 * 
 * @author sandipbose
 * 
 * This interface deals with storage of a message object
 * 1. Persist the Message to a storage
 * 2. Retrieve the message from the storage
 * 3. Delete the message from the storage
 */
public interface MessageStorage {
	public int storeMessage(Message msg);
	public Message retreiveMessage(MessageUID messageUID);
	public boolean deleteMessage(Message msg);
}
