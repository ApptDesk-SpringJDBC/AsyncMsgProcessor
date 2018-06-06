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
<%@ page import ="java.util.Random"%>;

<%@ page import ="org.jpos.iso.ISODate"%>;
<%@ page import ="org.jpos.iso.ISOException"%>;
<%@ page import ="org.jpos.iso.ISOField"%>;
<%@ page import ="org.jpos.iso.ISOMsg"%>;
<%@ page import ="org.jpos.iso.ISOPackager"%>;
<%@ page import ="org.jpos.iso.ISOUtil"%>;
<%@ page import ="org.jpos.iso.packager.ISO87APackager"%>;
<%@ page import ="com.ca.asynchmsg.iso.CAISO87APackager"%>;
<%@ page import ="org.jpos.iso.IFA_LLCHAR"%>;
<%@ page import ="org.jpos.iso.IF_CHAR"%>;
<%@ page import ="org.jpos.iso.ISOFieldPackager"%>;
<%@ page import ="org.jpos.iso.ISOField"%>;
<%@ page import ="com.ca.asynchmsg.message.MessageUIDGenerator"%>;
<%@ page import ="com.ca.asynchmsg.connectionset.ConnectionPool"%>;
<%@ page import ="com.ca.asynchmsg.container.ServerContainer"%>;


<%!

	static final String hostIP = "HOST";
	static final String portIP= "PORT";

%>

<%!

public String create0200Request(String cardnumber, String expDate, String encPin){
	/**
	 *   ISO8583 Message structure Construction
	 *   Packager : ASCII  ISO87APackager
	 */
	String reqStr = null;
	Date d = new Date();
	ISOMsg  req = new ISOMsg();
	req.setPackager (new ISO87APackager());

	String stan = "100000";

	long dataVal = new Date().getTime();
	String rrNumber = "" + dataVal / 10 ;

	stan = generateRandom(6);
	try
	{
		req.setMTI ("0200");
		req.set(2,cardnumber);
		req.set(3,"300000");
		req.set (7,ISODate.getDate(d)+ISODate.getTime(d));
		req.set (11,stan);
		req.set (12,ISODate.getTime(d));
		req.set (13,ISODate.getDate(d));
		req.set (14, expDate);
		req.set (32, "00000005");
		req.set (35, cardnumber+"="+expDate);
		req.set (37, rrNumber);
		req.set (41, "ARCOT100");
		req.set (42, "000000000000000");
		req.set (43, "ARCOT SYSTEMS INCORPORATED BANGALORE IND");
		req.set (49, "356");
		req.set(52,ISOUtil.hex2byte(encPin));
		reqStr = new String(req.pack());
	} catch (ISOException e1) {
		System.err.println("ISO Request Message Construction Exception ->" + e1);
		return  null;
	}
	return reqStr;
}

public String create0100Request(String cardnumber, String expDate, String encPin){
	/**
	 *   ISO8583 Message structure Construction
	 *   Packager : ASCII  ISO87APackager
	 */
	String reqStr = null;
	Date d = new Date();
	ISOMsg  req = new ISOMsg();
	req.setPackager (new ISO87APackager());

	String stan = "100000";

	long dataVal = new Date().getTime();
	String rrNumber = "" + dataVal / 10 ;

	stan = generateRandom(6);
		try
		{
			ISO87APackager packager = new ISO87APackager();

			ISOFieldPackager otpFieldPackager = new IFA_LLCHAR();
			otpFieldPackager.setLength(6);
			packager.setFieldPackager(67, otpFieldPackager);
			req.setPackager(packager);
			req.setMTI("0100");
			req.set(new ISOField(2, cardnumber));
			req.set(new ISOField(3, "300000"));
			
			req.set(new ISOField(4, "000000000000"));

			String date1 = new StringBuilder(String.valueOf(ISODate.getDate(d)))
					.append(ISODate.getTime(d)).toString();
			req.set(new ISOField(7, ISODate.getDate(d)+ISODate.getTime(d)));
			req.set(new ISOField(11, stan));

			req.set(new ISOField(12, ISODate.getTime(d)));
			req.set(new ISOField(13, ISODate.getDate(d)));
			req.set(new ISOField(14, expDate));
			
			ISOFieldPackager acqrIdFieldPackager = new IFA_LLCHAR();
			acqrIdFieldPackager.setLength(20);
			packager.setFieldPackager(32, acqrIdFieldPackager);
			
			req.set(new ISOField(32, "00000005"));

			req.set(new ISOField(37, rrNumber));
			
			ISOFieldPackager termIdPackager = new IF_CHAR();
			termIdPackager.setLength(16);
			packager.setFieldPackager(41, termIdPackager);
			
			req.set(new ISOField(41, "ARCOT100"));
			req.set(new ISOField(42, "000000000000000"));
			req.set(new ISOField(43, "ARCOT Online Shopping Portal"));
			req.set(new ISOField(49, "356"));
			req.set(new ISOField(67, "123456"));
			reqStr = new String(req.pack());
		} catch (ISOException e1) {
		System.err.println("ISO Request Message Construction Exception ->" + e1);
		return  null;
	}
	return reqStr;
}

public static String generateRandom(int length) {
    Random random = new Random();
    char[] digits = new char[length];
    digits[0] = (char) (random.nextInt(9) + '1');
    for (int i = 1; i < length; i++) {
        digits[i] = (char) (random.nextInt(10) + '0');
    }
    return new String(digits);
}


%>

<%
	out.clearBuffer();
	try{
		String host = "";
		String port = "";
		
		String remoteHostName = request.getRemoteHost();
		String remoteHostIP = request.getRemoteAddr();
		if(	remoteHostName == null || remoteHostIP == null ||
			( ! ( (remoteHostName.equalsIgnoreCase("localhost") || remoteHostName.equalsIgnoreCase("127.0.0.1")
					|| remoteHostName.equalsIgnoreCase("0:0:0:0:0:0:0:1"))
			&& (remoteHostIP.equalsIgnoreCase("127.0.0.1") || remoteHostIP.equalsIgnoreCase("0:0:0:0:0:0:0:1")))))
		{
			out.write("You can access this feature only from localhost");
			return;
		}

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
		MessageUIDGenerator messageUIDGenerator = ConnectionPool.getHandleToContainer(host+"_"+port).getMessageUIDGenerator();
		ISOMsg res = new ISOMsg();
		res.setPackager(new CAISO87APackager(messageUIDGenerator));


		String requestStr = null;
		String messageReqStan = null;
		String messageReqCn = null;

		requestStr = create0200Request("4544567890123456", "1510", "1231211112312111");
		//requestStr = create0100Request("4544567890123456", "1510", "1231211112312111");
		if (requestStr == null){
			out.write("Request Created is NULL");
			return;
		}else{
			res.unpack(requestStr.getBytes());
			messageReqStan = res.getString(11);
			messageReqCn = res.getString(2);
		}

		ATMSwitchTCPHandler tcpHandler = new ATMSwitchTCPHandler();

		String responseVal = null;
		String messageResStan = null;
		String messageResCn = null;

		responseVal = tcpHandler.processRequest(host+"_"+port,requestStr);
		if (responseVal == null){
			out.write("Response Rcvd is NULL");
			return;
		}else{
			res.unpack(responseVal.getBytes());
			messageResStan = res.getString(11);
			messageResCn = res.getString(2);
		}

%>
		<html>
			<table border=1>
				<tr>
					<td colspan ="2" align="center"> Test Results </td>
				</tr>
				<tr>
					<td>HOST</td>
					<td>
						<%= host%>
					</td>
				</tr>
				<tr>
					<td>PORT</td>
					<td>
						<%= port%>
					</td>
				</tr>
				<tr>
					<td>[STAN,CN]REQUEST</td>
					<td>
						[<%= messageReqStan%>,<%= messageReqCn%>]&nbsp;<%= requestStr%>
					</td>
				</tr>
				<tr>
					<td>[STAN,CN]RESPONSE</td>
					<td>
						[<%= messageResStan%>,<%= messageResCn%>]&nbsp;<%= responseVal%>
					</td>
				</tr>
			</table>
		</html>
<%

	}
	catch(Exception e)
	{
		out.write("Exception: "+e.toString());
		return;

	}

%>
