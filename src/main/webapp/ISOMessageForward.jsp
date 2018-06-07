<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<%@ page import="com.ca.asynchmsg.initializer.ATMSwitchTCPHandler"%>
<%@ page import="java.io.BufferedReader"%>
<%@ page import="java.io.IOException"%>
<%@ page import="java.io.UnsupportedEncodingException"%>
<%@ page import="java.net.URLEncoder"%>
<%@ page import="java.security.*"%>
<%@ page import="java.util.ArrayList"%>
<%@ page import="java.util.Date"%>
<%@ page import="java.util.HashMap"%>
<%@ page import="java.util.StringTokenizer"%>
<%@ page import="java.util.regex.Matcher"%>
<%@ page import="java.util.regex.Pattern"%>
<%@ page import="java.net.*"%>
<%@ page import="java.io.*"%>
<%@ page import="javax.servlet.http.*"%>;
<%@ page import ="java.io.IOException"%>;

<%!

	static final String ISOMSGREQ = "ISOREQ";
	static final String hostIP = "HOST";
	static final String portIP= "PORT";
%>

<%
	out.clearBuffer();
	try{
		String host = "";
		String port = "";
		String isoReq = "";

		if(request.getParameter(hostIP)!=null && !"".equals(request.getParameter(hostIP))){
			host = request.getParameter(hostIP);
		}else{
			out.write("HOST IS NULL");
			return;
		}

		if(request.getParameter(portIP)!=null && !"".equals(request.getParameter(portIP))){
			port = request.getParameter(portIP);
		}else{
			out.write("PORT IS NULL");
			return;
		}

		if(request.getParameter(ISOMSGREQ)!=null && !"".equals(request.getParameter(ISOMSGREQ))){
			isoReq = request.getParameter(ISOMSGREQ);
		}else{
			out.write("ISOMSGREQ IS NULL");
			return;
		}
		String responseVal = ATMSwitchTCPHandler.processRequest(host+"_"+port,isoReq);

		if(responseVal == null){
			out.write("Exception: Response Null");
		} else if (responseVal.indexOf("Exception") > -1 ){
			out.write(responseVal);
		} else {
			out.write(responseVal);
		}

	}
	catch(Exception e)
	{
		out.write("Exception:"+e.toString());
		return;

	}

%>
