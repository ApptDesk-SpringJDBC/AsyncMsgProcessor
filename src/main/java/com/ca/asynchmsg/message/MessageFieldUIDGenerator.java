package com.ca.asynchmsg.message;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jpos.iso.ISOMsg;

/**
 * 
 * @author upara01
 * MessageFieldUIDGenerator is an implementation class of MessageUIDGenerator.
 * It Generates MessageFiels UID very dynamically. A different connection can have different logic to generate messageUID 
 */
public class MessageFieldUIDGenerator implements MessageUIDGenerator{

	private int[] ISOMsgFields;
	private Set<Integer> maskedFields = new HashSet<Integer>(Arrays.asList(2));
	
	
	MessageFieldUIDGenerator(int[] fields){
		ISOMsgFields = fields;
	}
	
	public int[] getISOMsgFields() {
		return this.ISOMsgFields;
	}

	/**
	 * generateMessageUID(ISOMsg) : Generates MessageUID.
	 */
	@Override
	public MessageUID generateMessageUID(ISOMsg isoMsg) throws Exception {
		MessageUID messageUID = new MessageUID();
		
		String uidString = "";
		String maskedUid = "";
		for (int i = 0; i < ISOMsgFields.length; i++) {
			
			int fldNo = ISOMsgFields[i];
			if (isoMsg.hasField(fldNo)) {
				
				String fieldData = isoMsg.getString(fldNo);
				uidString = uidString + fieldData;
				
				String maskedString = getMaskedString(fieldData, fldNo);
				maskedUid = maskedUid + maskedString;
			} else {
				throw new Exception("Exception occured while generating MessageUID. Field Number : "+ fldNo+ " not found");
			}

		}
		messageUID.setUID(uidString);
		messageUID.setMaskedUID(maskedUid);
		return messageUID;

	}
	
	/**
	 * getMaskedString(String) : It provides Masked messageUid for logging purpose.
	 * @param plainData
	 * @param fieldNo
	 * @return Returns Masked Message UID.
	 */
	private String getMaskedString(String plainData, int fieldNo) {
		String maskedString = plainData;
		
		if(isMasked(fieldNo)) {
			if(fieldNo == 2) {
				int len = plainData.length();
				StringBuffer sb = new StringBuffer();
				for(int i =0;i<=len-4;i++){
					sb.append("X");
				}
				sb.append(plainData.substring(len-4));
				maskedString = sb.toString();
			}
		}
		
		return maskedString;
	}
	private boolean isMasked(int fieldNo) {
		
		boolean doMask = false;
		if(maskedFields.contains(fieldNo)){
			doMask = true;
		}
		return doMask;
	}
}
