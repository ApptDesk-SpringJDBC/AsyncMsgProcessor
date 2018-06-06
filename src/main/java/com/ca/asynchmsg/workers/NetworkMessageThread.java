package com.ca.asynchmsg.workers;

import java.util.Date;
import java.util.Random;

import org.apache.log4j.Logger;
import org.jpos.iso.ISODate;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.ISO87APackager;

import com.ca.asynchmsg.beans.ConnectionBean;
import com.ca.asynchmsg.initializer.ATMSwitchTCPHandler;
/**
 * @author upara01
 *
 * NetworkMessageThread is a thread that will process SIGNON/SIGNOFF and schedule ECHO test.
 */
public class NetworkMessageThread extends Thread {
	 
	private ConnectionBean connBean;
	private Logger logger;
	private boolean stopRequested = false;
	public static String signOn = "SIGNON";
	public static String echo = "ECHO";
	public static String signOff = "SIGNOFF";
	
	
	public NetworkMessageThread(ConnectionBean connBean, Logger logger) {
		 	this.connBean = connBean;
		 	this.logger = logger;
		 	
	}
	/**
	 * handleNetworkManagementMsg() method is responsible to process the network related messages.
	 * @param strNetworkMsgName : This is a network message.
	 * @return Returns boolean True in case it was successful otherwise false
	 */
	public boolean handleNetworkManagementMsg(String strNetworkMsgName){
		ISOMsg  req = new ISOMsg();
		req.setPackager (new ISO87APackager());

		ISOMsg res = new ISOMsg();
		res.setPackager(new ISO87APackager());

		Date d = new Date();
		String stan = "";
		try {
			req.setMTI("0800");
			req.set(7,ISODate.getDate(d)+ISODate.getTime(d));
			stan = generateRandom(6);
			req.set(11,stan);
			if (signOn.equals(strNetworkMsgName)){
				req.set(70,"001");
			}else if(echo.equals(strNetworkMsgName)){
				req.set(70,"301");
			}else if(signOff.equals(strNetworkMsgName)){
				req.set(70,"002");
			}else{}
			logger.info(" Attempting ( handleNetworkManagementMsg() ) " + strNetworkMsgName + " ( " + stan + " )");
		} catch (ISOException e1) {
			logger.error(" ( handleNetworkManagementMsg() ) ISOException ",e1);
			return false;
		}
		String nwMessageResp = null;	
		try {
			if(!stopRequested)
				nwMessageResp = new ATMSwitchTCPHandler().processRequest(connBean.getUniqueName(), new String(req.pack()), stan);
		} catch (Exception e) {
			logger.error(" ( handleNetworkManagementMsg() ) Exception ",e);
			return false;
		}
		if(nwMessageResp != null){
			try {
				res.unpack(nwMessageResp.getBytes());
				if("0810".equals(res.getMTI()) && "00".equals(res.getValue(39))){
					logger.info(" Successful ( handleNetworkManagementMsg() ) " + strNetworkMsgName + " ( " + stan + " )");
					return true;
				}else{
					logger.error(" Failed ( handleNetworkManagementMsg() ) " + strNetworkMsgName + " ( " + stan + " )");
				}
			} catch (ISOException e) {
				logger.error(" ( handleNetworkManagementMsg() ) ISOException ",e);
			}
		}else{
			
		}
		return false;			
	}
	/**
	 * generateRandom() method is used to generate and return random numbers of fixed length.
	 * @param length: The length of which Random numbers will be generated.
	 * @return Returns Random numbers
	 */
	private String generateRandom(int length) {
	    Random random = new Random();
	    char[] digits = new char[length];
	    digits[0] = (char) (random.nextInt(9) + '1');
	    for (int i = 1; i < length; i++) {
	        digits[i] = (char) (random.nextInt(10) + '0');
	    }
	    return new String(digits);
	}
	
	public void run() {
		try {
			while(!stopRequested) {				
				try {
					Thread.sleep(connBean.getEchoInterval() * 1000 * 60);
					handleNetworkManagementMsg(echo);
				} catch (InterruptedException e) {
					logger.warn(" ( NetworkMessageThread ) InterruptedException. The NetworkMessageThread was Interrupted....");
					//Thread.sleep(10 * 1000 * 60);
				}
			}
		} catch (Exception e) {
			logger.error(" ( NetworkMessageThread ) Exception. Issue with ECHO....",e);
		}
	 }
	
	/**
	 * requestStop() : Stops the NetworkMessageThread {@link NetworkMessageThread}
	 */
	public void requestStop() {
		logger.info("NetworkMessageThread is going to stop....");
		stopRequested = true;
	}
}
