package com;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.jpos.iso.ISODate;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.ISO87APackager;

public class TCPMServer {
	public static void main (String[] args) {
	  ServerSocket server = null;
	  Logger logger = Logger.getLogger(TCPMServer.class);
	  DailyRollingFileAppender fa = new DailyRollingFileAppender();
	  fa.setFile("/tmp/logs/ISOStub/Serverlocalhost_"+args[0]+"_logs");
	  fa.setLayout(new PatternLayout("%d | %-5p | [%t:%c{1}] | %m%n"));
	  fa.setDatePattern("'.'yyyy-MM-dd");
	  //fa.setThreshold(Level.TRACE);
	  fa.setAppend(true);
	  fa.activateOptions();
	  
	  logger.setLevel(Level.TRACE);
	  logger.addAppender(fa);
	  
	  try {
		  logger.info("Trying to start server on Port [ "+args[0]+" ]");
		  server = new ServerSocket(Integer.parseInt(args[0]));	
		  while (true) {
	        Socket client = server.accept();
	        if (client.isConnected()){
	        	logger.info("Started listening on Port [ "+args[0]+" ]");
	        }
	        EchoHandler handler = new EchoHandler(client, logger);
	        handler.start();
	      }
    }
    catch (Exception e) {
    	logger.error("Exception in starting server at Port [ "+args[0]+" ]", e);
    	//System.err.println("Exception caught:" + e);
    }finally{
 
    }
    
  }
}
 
class EchoHandler extends Thread {
  Socket client;
  Logger logger;
  EchoHandler (Socket client, Logger logger) {
    this.client = client;
    this.logger = logger;
  }
  public void run () {
	InputStream is = null;
	OutputStream os = null;
	try {
      is = client.getInputStream();
      os = client.getOutputStream();
      PrintWriter writer = new PrintWriter(os, true);
 
      while (true) {
 		String strRspMessage = fetchRequestFromClient(is,client);
		logger.info("[ "+client.toString()+" ] Length of Response : "+ strRspMessage.length());
		byte[] byteBuffer = new byte[strRspMessage.length() + 2];
		int length = strRspMessage.length();
		if (length < 256){
			byteBuffer[0] = (byte)0;
			byteBuffer[1] = (byte)length;
		}else{
			byteBuffer[0] = (byte)(length / 256);
			byteBuffer[1] = (byte)(length % 256);
		}
		byte[] reqBytes = strRspMessage.getBytes();
		for (int j = 2; j < byteBuffer.length; j++) {
			byteBuffer[j] = reqBytes[j - 2];
		}
		
		os.write(byteBuffer, 0, byteBuffer.length);
		os.flush();
      }
    }
    catch (Exception e) {
    	logger.error("Exception caught: client disconnected.", e);
    }
    finally {
      try { 
    	  client.close(); 
      }
      catch (Exception e ){ 
    	  logger.error("Exception caught: while closing the client connection.", e);
      }
    }
  }
  
  private synchronized String fetchRequestFromClient(InputStream is, Socket client) throws IOException, ISOException{
	  
      String messageString = "";
      DataInputStream in = new DataInputStream(is);
      byte byteOne = in.readByte();
      byte byteTwo = in.readByte();
      
      
      int msgLength = (int)byteTwo;
      if (msgLength < 0){
    	  msgLength = 256 + msgLength;
      }
      
      //logger.info("[ "+client.toString()+" ] Bytes : "+ byteOne + " , "+ byteTwo);
      
      byte[] messageByte = new byte[(int)msgLength];
      
      int bytesRead = 0;
      bytesRead = in.read(messageByte);
      messageString = new String(messageByte, 0, bytesRead);
      
      ISOMsg req = new ISOMsg();
      req.setPackager(new ISO87APackager());
      req.unpack(messageString.getBytes());
      
      ISOMsg res = new ISOMsg();
	  res.setPackager(new ISO87APackager());
	  
	  logger.info("[ "+client.toString()+" ] Request Rcv : [ " + req.getString(11) + " ] "+messageString);
	  
	  Date d = new Date();
      if ("0800".equals(req.getMTI())){
    	  res.setMTI("0810");
    	  res.set(7,ISODate.getDate(d)+ISODate.getTime(d));
    	  res.set(11,req.getString(11));
    	  res.set(39,"00");
    	  res.set(70,req.getString(70));
    	  messageString = new String(res.pack());

      } else if ("0200".equals(req.getMTI())){
  		// responses
    	
		res.setMTI("0210");
		res.set(2,req.getString(2));
		res.set(3,req.getString(3));
		res.set(7,ISODate.getDate(d)+ISODate.getTime(d));
		res.set(11,req.getString(11));
		res.set(12,ISODate.getTime(d));
		res.set(13,ISODate.getDate(d));
		res.set(14,req.getString(14));
		res.set(32,req.getString(32));
		res.set(35,req.getString(35));
		res.set(37,req.getString(37));
		res.set(41,req.getString(41));
		res.set(42,req.getString(42));
		res.set(43,req.getString(43));
		res.set(49,req.getString(49));
		
		if ("1510".equals(req.getString(14))){
			// success
			res.set(39,"00");
		}else{
			// fail
			res.set(39,"01");
		}
      }
      
      messageString = new String(res.pack());
      logger.info("[ "+client.toString()+" ] Sending Back : [ " + res.getString(11) + " ] "+messageString);
      return messageString;
  }
}