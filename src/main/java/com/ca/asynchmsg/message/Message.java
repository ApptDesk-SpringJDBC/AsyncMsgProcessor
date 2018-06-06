package com.ca.asynchmsg.message;

/**
 * 
 * @author sandipbose
 * 
 * Base Class which defines the contents of a Message Object and Contains
 * 1. Message Body
 * 2. Message Unique ID
 */
public class Message {
	
	private MessageUID messageUID;
	private String messageContents;
	
	public Message(){}
	public Message(String messageContents, MessageUID messageUID){
		this.messageContents = messageContents;
		this.messageUID = messageUID;
	}
	
	public String getMessageContents() {
		return messageContents;
	}
	public void setMessageContents(String messageContents) {
		this.messageContents = messageContents;
	}
	public MessageUID getMessageUID() {
		return messageUID;
	}
	public void setMessageUID(MessageUID messageUID) {
		this.messageUID = messageUID;
	}

}
