package com.ca.asynchmsg.message;

import org.jpos.iso.ISOMsg;
/**
 * 
 * @author upara01
 *MessageUIDGenerator is an interface which is implemented by {@link MessageFieldUIDGenerator}
 */
public interface MessageUIDGenerator {
	
	public MessageUID generateMessageUID(ISOMsg isoMsg) throws Exception;

	public int[] getISOMsgFields();
	
}
