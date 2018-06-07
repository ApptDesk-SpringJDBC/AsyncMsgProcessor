package com.ca.asynchmsg.initializer;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.net.InetAddress;

@WebServlet(urlPatterns = {"/refreshISOBackend","/getjsonFile","/updateBackendParams","/stopISOBackend","/saveNewBackend"})
public class AsynchMessageServlet extends HttpServlet {
	private static final long serialVersionUID =1L;
	private ServletContext m_ctx;
	private static final String serverHostKey = "host";
	private static final String portKey = "port";
	private static final String connTimeOutKey = "connTimeOut";
	private static final String readTimeOutKey = "readTimeOut";
	private static final String threadTimeOutKey = "threadTimeOut";
	private static final String retryKey = "retry";
	private static final String loglevelKey = "logLevel";
	private static final String echoTimeIntervalKey = "echoTimeInterval";
	private static final String atmSwitchConfigFileName = "atmswitchconfig.json";
	private static final String serverConfigKey = "serverConfig";
	private static final String uidFormat = "uidFormat";
	private static final String logFileLocationKey = "logFileLocation";

	public void init(ServletConfig conf) throws ServletException{
		super.init(conf);
		m_ctx = conf.getServletContext();
	}
	
	/**Process the HTTP Get request*/
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}
	
	/**Process the HTTP Post request*/
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{	
		String host = "";
		String port = "";	
		
		PrintWriter out = response.getWriter();
		if(request.getRequestURI().contains("refreshISOBackend")){			
			try {
				if(validateHostandPort(request,response)){
					
					if(request.getParameter(serverHostKey)!=null && !"".equals(request.getParameter(serverHostKey)))
						host = request.getParameter(serverHostKey);
					else{
						out.write("Host/IP is null or Blank");
						return;
					}
					if(request.getParameter(portKey)!=null && !"".equals(request.getParameter(portKey)))
						port = request.getParameter(portKey);
					ATMSwitchTCPHandler tcpHandler = new ATMSwitchTCPHandler();
					String responseVal = tcpHandler.processRequest(host+"_"+port,"reconnectCall");
					ATMSwitchTCPHandler.manualStopPool.put(host+"_"+port, Boolean.FALSE);
	
					if(responseVal == null){
						out.write("RefreshModule Issue: Response Null");
					} else {
						out.write(responseVal);
					}
					
					return ;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}else if(request.getRequestURI().contains("stopISOBackend")){
			try{
				if(validateHostandPort(request,response)){
					String connId =request.getParameter(serverHostKey)+"_"+request.getParameter(portKey);
					if(ATMSwitchTCPHandler.cleanUpResources(connId, false)){						
						out.write(connId+" : Stopped Successfully....");
					}
					else{
						out.write(connId+" : could not be Stopped! Seems it is already Stopped...");
					}
					if(ATMSwitchTCPHandler.manualStopPool.containsKey(connId)){
						ATMSwitchTCPHandler.manualStopPool.replace(connId,Boolean.TRUE);
					}
					ATMSwitchTCPHandler.manualStopPool.put(host+"_"+port, Boolean.TRUE);
				}
			}catch (Exception e) {
				e.printStackTrace();
			}
		}else if(request.getRequestURI().contains("getjsonFile")){
			//JsonObject allConfigs = Json.createReader(request.getServletContext().getResourceAsStream("atmswitchconfig.json")).readObject();
			InputStream instr = Thread.currentThread().getContextClassLoader().getResource(atmSwitchConfigFileName).openStream();
			JsonObject allConfigs = Json.createReader(instr).readObject();
			
			response.setContentType("application/json");
			out.print(allConfigs);
			out.flush();
		}else if(request.getRequestURI().contains("updateBackendParams")){
			try {				
				if(validateHostandPort(request,response) && validateOtherParams(request,response)){
					if(request.getParameter(serverHostKey)!=null && !"".equals(request.getParameter(serverHostKey)))
						host = request.getParameter(serverHostKey);
					if(request.getParameter(portKey)!=null && !"".equals(request.getParameter(portKey)))
						port = request.getParameter(portKey);
					if(request.getParameter(connTimeOutKey)!=null && !"".equals(request.getParameter(connTimeOutKey)))
						port = request.getParameter(portKey);
					
					ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
					InputStream instr = classLoader.getResource(atmSwitchConfigFileName).openStream();
					JsonReader jsonReader = Json.createReader(instr);
					JsonObject allConfigs = jsonReader.readObject();
					jsonReader.close();
					instr.close();
					JsonObjectBuilder backendBuilder = Json.createObjectBuilder();
					JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
					JsonArray serverConfigs = allConfigs.getJsonArray(serverConfigKey);
					for(int i =0;i<serverConfigs.size();i++){
						JsonObject serverConfig = serverConfigs.getJsonObject(i);
						if(host.equalsIgnoreCase(serverConfig.getJsonString(serverHostKey).getString()) && Integer.valueOf(port) == serverConfig.getJsonNumber(portKey).intValue()){
							arrayBuilder.add(Json.createObjectBuilder()
									.add(serverHostKey, host)
			                    	.add(portKey, Integer.valueOf(port))
			                        .add(connTimeOutKey, Integer.valueOf(request.getParameter(connTimeOutKey)))
			                        .add(readTimeOutKey, Integer.valueOf(request.getParameter(readTimeOutKey)))
									.add(threadTimeOutKey, Integer.valueOf(request.getParameter(threadTimeOutKey)))
									.add(retryKey, Integer.valueOf(request.getParameter(retryKey)))
									.add(loglevelKey, request.getParameter(loglevelKey))
									.add(echoTimeIntervalKey, Integer.valueOf(request.getParameter(echoTimeIntervalKey)))
									.add(uidFormat, serverConfig.getJsonString(uidFormat).getString()));
						}else{
							arrayBuilder.add(Json.createObjectBuilder()
									.add(serverHostKey, serverConfig.getJsonString(serverHostKey).getString())
			                    	.add(portKey, serverConfig.getJsonNumber(portKey).intValue())
			                        .add(connTimeOutKey, serverConfig.getJsonNumber(connTimeOutKey).intValue())
			                        .add(readTimeOutKey, serverConfig.getJsonNumber(readTimeOutKey).intValue())
									.add(threadTimeOutKey, serverConfig.getJsonNumber(threadTimeOutKey).intValue())
									.add(retryKey, serverConfig.getJsonNumber(retryKey).intValue())
									.add(loglevelKey, serverConfig.getJsonString(loglevelKey).getString())
									.add(echoTimeIntervalKey, serverConfig.getJsonNumber(echoTimeIntervalKey).intValue())
									.add(uidFormat, serverConfig.getJsonString(uidFormat).getString()));							
						}
						
					}
					backendBuilder.add(serverConfigKey,arrayBuilder)
								  .add(logFileLocationKey, allConfigs.getJsonString(logFileLocationKey).getString());
					
					JsonObject jsonObject =backendBuilder.build();
					
					OutputStream os = new FileOutputStream(classLoader.getResource(atmSwitchConfigFileName).getFile());
					JsonWriter jsonWriter = Json.createWriter(os);
					jsonWriter.writeObject(jsonObject);
			        jsonWriter.close();
			        out.write("ISO Backend Parameters are updated successfully");
			        out.flush();
				}
			} 
			catch (Exception e) {
				out.write("ISO Backend Parameters Update Failed");
				e.printStackTrace();
			}
		}else if(request.getRequestURI().contains("saveNewBackend")){
				try {				
					if(validateHostandPort(request,response) && validateOtherParams(request,response)){
						if(request.getParameter(serverHostKey)!=null && !"".equals(request.getParameter(serverHostKey)))
							host = request.getParameter(serverHostKey);
						if(request.getParameter(portKey)!=null && !"".equals(request.getParameter(portKey)))
							port = request.getParameter(portKey);
						if(request.getParameter(connTimeOutKey)!=null && !"".equals(request.getParameter(connTimeOutKey)))
							port = request.getParameter(portKey);
						
						ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
						InputStream instr = classLoader.getResource(atmSwitchConfigFileName).openStream();
						JsonReader jsonReader = Json.createReader(instr);
						JsonObject allConfigs = jsonReader.readObject();
						jsonReader.close();
						instr.close();
						JsonObjectBuilder backendBuilder = Json.createObjectBuilder();
						JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
						JsonArray serverConfigs = allConfigs.getJsonArray(serverConfigKey);
						String uidFormatValue = "";
						for(int i =0;i<serverConfigs.size();i++){
							JsonObject serverConfig = serverConfigs.getJsonObject(i);
							uidFormatValue = serverConfig.getJsonString(uidFormat).getString();
							if(host.equalsIgnoreCase(serverConfig.getJsonString(serverHostKey).getString()) && Integer.valueOf(port) == serverConfig.getJsonNumber(portKey).intValue()){
								out.write(" Error: Backend already exists. Please enter unique Backend! ");
								return;
							}else{
								arrayBuilder.add(Json.createObjectBuilder()
										.add(serverHostKey, serverConfig.getJsonString(serverHostKey).getString())
				                    	.add(portKey, serverConfig.getJsonNumber(portKey).intValue())
				                        .add(connTimeOutKey, serverConfig.getJsonNumber(connTimeOutKey).intValue())
				                        .add(readTimeOutKey, serverConfig.getJsonNumber(readTimeOutKey).intValue())
										.add(threadTimeOutKey, serverConfig.getJsonNumber(threadTimeOutKey).intValue())
										.add(retryKey, serverConfig.getJsonNumber(retryKey).intValue())
										.add(loglevelKey, serverConfig.getJsonString(loglevelKey).getString())
										.add(echoTimeIntervalKey, serverConfig.getJsonNumber(echoTimeIntervalKey).intValue())
										.add(uidFormat, serverConfig.getJsonString(uidFormat).getString()));							
							}							
						}
						arrayBuilder.add(Json.createObjectBuilder()
								.add(serverHostKey, host)
		                    	.add(portKey, Integer.valueOf(port))
		                        .add(connTimeOutKey, Integer.valueOf(request.getParameter(connTimeOutKey)))
		                        .add(readTimeOutKey, Integer.valueOf(request.getParameter(readTimeOutKey)))
								.add(threadTimeOutKey, Integer.valueOf(request.getParameter(threadTimeOutKey)))
								.add(retryKey, Integer.valueOf(request.getParameter(retryKey)))
								.add(loglevelKey, request.getParameter(loglevelKey))
								.add(echoTimeIntervalKey, Integer.valueOf(request.getParameter(echoTimeIntervalKey)))
								.add(uidFormat, uidFormatValue));
						backendBuilder.add(serverConfigKey,arrayBuilder)
									  .add(logFileLocationKey, allConfigs.getJsonString(logFileLocationKey).getString());
						
						JsonObject jsonObject =backendBuilder.build();
						
						OutputStream os = new FileOutputStream(classLoader.getResource(atmSwitchConfigFileName).getFile());
						JsonWriter jsonWriter = Json.createWriter(os);
						jsonWriter.writeObject(jsonObject);
				        jsonWriter.close();
				        out.write("ISO Backend saved successfully");
				        out.flush();
					}
				} catch (Exception e) {
					out.write("ISO Backend save Failed");
					e.printStackTrace();
				}
		}
	}
	
	private boolean validateHostandPort(HttpServletRequest request, HttpServletResponse response) throws Exception{
		PrintWriter out = response.getWriter();
		String remoteHostName = request.getRemoteHost();
		String remoteHostIP = request.getRemoteAddr();
		String reHostName = InetAddress.getByName(remoteHostIP).getHostName();
		if(	remoteHostName == null || remoteHostIP == null ||
			( ! (remoteHostName.equalsIgnoreCase("localhost") || remoteHostName.equalsIgnoreCase("127.0.0.1")
					|| remoteHostName.equalsIgnoreCase("0:0:0:0:0:0:0:1") || remoteHostName.equalsIgnoreCase("preview5.arcot.com") || reHostName.equalsIgnoreCase("preview5.arcot.com"))))
		{
			out.write("You can access this feature only from localhost");
			return false;
		}

		if(request.getParameter(serverHostKey)!=null && !"".equals(request.getParameter(serverHostKey))){
			
		}else{
			out.write("HOST is NOT Mentioned!");
			return false;
		}
		String port = request.getParameter(portKey);
		if(port!=null && !"".equals(port)){
			try{
			Integer.valueOf(port);
			}catch(NumberFormatException nfe){
				out.write("Port: Please enter a number!");
				return false;
			}
		}else{
			out.write("PORT is NOT Mentioned!");
			return false;
		}
		return true;
	}
	
	private boolean validateOtherParams(HttpServletRequest request, HttpServletResponse response) throws Exception{
		boolean returnVal = false;
		PrintWriter out = response.getWriter();
		try{
			String connTimeout = request.getParameter(connTimeOutKey);
			if(!"".equals(connTimeout) && connTimeout != null){
				Integer.valueOf(connTimeout);
				returnVal =true;
			}else{
				out.write("Connection TimeOut is not Mentioned!");
				return false;
			}
		}catch(NumberFormatException nfe){
			out.write("Connection TimeOut: Please enter a number!");
			return false;
		}
		try{
			String readTimeout = request.getParameter(readTimeOutKey);
			if(!"".equals(readTimeout) && readTimeout != null){
				Integer.valueOf(readTimeout);
				returnVal =true;
			}else{
				out.write("Read TimeOut is not Mentioned!");
				return false;
			}
		}catch(NumberFormatException nfe){
			out.write("Read TimeOut: Please enter a number!");
			return false;
		}
		try{
			String threadTimeout = request.getParameter(threadTimeOutKey);
			if(!"".equals(threadTimeout) && threadTimeout != null){
				Integer.valueOf(threadTimeout);
				returnVal =true;
			}else{
				out.write("Thread TimeOut is not Mentioned!");
				return false;
			}
		}catch(NumberFormatException nfe){
			out.write("Thread TimeOut: Please enter a number!");
			return false;
		}
		try{
			String retry = request.getParameter(retryKey);
			if(!"".equals(retry) && retry != null){
				Integer.valueOf(retry);
				returnVal =true;
			}else{
				out.write("retry is not Mentioned!");
				return false;
			}
		}catch(NumberFormatException nfe){
			out.write("retry: Please enter a number!");
			return false;
		}
		try{
			String echoTime = request.getParameter(echoTimeIntervalKey);
			if(!"".equals(echoTime) && echoTime != null){
				Integer.valueOf(echoTime);
				returnVal =true;
			}else{
				out.write("Echo Time Interval is not Mentioned!");
				return false;
			}
		}catch(NumberFormatException nfe){
			out.write("Echo Time Interval: Please enter a number!");
			return false;
		}
		
		return returnVal;		
	}
}