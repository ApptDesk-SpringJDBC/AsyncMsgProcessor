package com.ca.asynchmsg.message;

/**
 * 
 * @author upara01
 * MessageUIDGeneratorFactory class follows factory design pattern to generate and provide MessageUID
 */
public class MessageUIDGeneratorFactory {
	
	/**
	 * getMessageUIDGenerator(String) : It provides MessageUIDGenerator object at runtime.
	 * @param uidFormat : It is configurable and configured in json file and it is used to generate MessageUIDGenerator object.
	 * @return : Returns MessageUIDGenerator object
	 * @throws Exception
	 */
	public static MessageUIDGenerator getMessageUIDGenerator(String uidFormat) throws Exception {
		
		MessageUIDGenerator messageUIDGenerator = null;
		if(uidFormat == null){
			throw new Exception("Missing parameter uidFormat ");
		}
		boolean isCustom = uidFormat.startsWith("custom");
		
		if(!isCustom){
			try{
				String[] uid = uidFormat.split(",");
				int[] fieldNumbers = new int[uid.length]; 

				for(int i=0;i<uid.length;i++){
					fieldNumbers[i] = Integer.parseInt(uid[i]);
				}
				messageUIDGenerator = new MessageFieldUIDGenerator(fieldNumbers);
				return messageUIDGenerator;
				
			} catch(NumberFormatException e){

				throw new Exception("Unable to convert "+ uidFormat +" to field numbers" );
			}

		}
		else{
			String[] classNames = uidFormat.split(":");
			if(classNames.length !=2){
				throw new Exception("Unable to parse "+ uidFormat + " to custom className");
			}
			String className = classNames[1];
			Class<?> c = Class.forName(className);
			messageUIDGenerator = (MessageUIDGenerator) c.newInstance();
			
			
		}
		return messageUIDGenerator;
		
		
	}
	
}
