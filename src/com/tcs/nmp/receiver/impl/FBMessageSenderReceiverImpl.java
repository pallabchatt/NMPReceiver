package com.tcs.nmp.receiver.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.FacebookClient.AccessToken;
import com.restfb.Parameter;
import com.restfb.Version;
import com.restfb.types.Conversation;
import com.restfb.types.Message;
import com.restfb.types.User;
import com.restfb.types.send.IdMessageRecipient;
import com.restfb.types.send.SendResponse;
import com.tcs.nmp.dao.intf.BatchResultDAO;
import com.tcs.nmp.dao.intf.FBMessageDAO;
import com.tcs.nmp.exception.NMPException;
import com.tcs.nmp.receiver.intf.MessageSenderReceiverIntf;


@Component("fbMessageSenderReceiverImpl")
public class FBMessageSenderReceiverImpl implements MessageSenderReceiverIntf {
	
	
	private static final Logger LOGGER =  LogManager.getLogger(FBMessageSenderReceiverImpl.class);

	static String accessToken = "EAACEdEose0cBABG4ZAlfUhmzxlZAHKgY673gDP270q27WgWDBANIYbEQOOPCaQIzfW3Dne4KJ3jyYWucZBlUsdWJVnEyiAZBmyrNHTdnUuMhDtDG9vNFtRSX8I0ClaNqUEARWWHROu7TNBeUJhphsrZBqDD6DLJZC6QdslZAyvZAZC7rFuZAmefQl4M5SSFYnJ20wZD";

	FacebookClient facebookClient = new DefaultFacebookClient(accessToken,
			Version.UNVERSIONED);

	private final static String USER_AGENT = "Mozilla/5.0";

	@Autowired
	private BatchResultDAO batchResultDAO;
	
	@Autowired
	private FBMessageDAO fbMessageDAO;
	
	public FBMessageSenderReceiverImpl(){
		
		try{
			
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return new java.security.cert.X509Certificate[0];
			}

			public void checkClientTrusted(X509Certificate[] certs,
					String authType) {
			}

			public void checkServerTrusted(X509Certificate[] certs,
					String authType) {
			}
		} };
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		HttpsURLConnection
				.setDefaultSSLSocketFactory(sc.getSocketFactory());

		// Create all-trusting host name verifier
		HostnameVerifier allHostsValid = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};
		// Install the all-trusting host verifier
		HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
		}catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			LOGGER.error(e);
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			LOGGER.error(e);
		}
	}

	@Override
	public String getReply(String input,String convId, String name) throws NMPException {
		
		String reply = "";
		try{
			
			
			// send message to predict layer started
			String url = "http://localhost:8081/NMPProcessor/predict";
			
			List<NameValuePair> params = new LinkedList<NameValuePair>();
			
			if (input != null) {
				params.add(new BasicNameValuePair("input", String.valueOf(input)));
				params.add(new BasicNameValuePair("conversationId", convId));
				params.add(new BasicNameValuePair("name", name));
			}
			String paramString = URLEncodedUtils.format(params, "utf-8");
			
			URL obj = new URL(url + "?" + paramString);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();
			
			// optional default is GET
			con.setRequestMethod("GET");
			
			// add request header
			con.setRequestProperty("User-Agent", USER_AGENT);
			
			/*
			 * con.setDoOutput(true); DataOutputStream doOutpu1t = new
			 * DataOutputStream(con.getOutputStream());
			 * doOutpu1t.writeBytes(urlParam); doOutpu1t.flush(); doOutpu1t.close();
			 */
			LOGGER.debug("\nSending 'GET' request to URL : " + url);
			LOGGER.debug("URL Params :" + paramString);
			int responseCode = con.getResponseCode();
			LOGGER.debug("Response Code : " + responseCode);
			
			if (responseCode == HttpURLConnection.HTTP_OK) { // success
				BufferedReader in = new BufferedReader(new InputStreamReader(
						con.getInputStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();
				
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();
				
				// print result
				LOGGER.debug("Response" + response.toString());
				reply = response.toString();
			} else {
				LOGGER.debug("POST request not worked");
			}
		}catch(Exception e){
			throw new NMPException(e.getMessage());
		}

		// print result

		return reply;

	}

	@Override
	public void receive() {
			try{
			
				User user = (User) facebookClient.fetchObject("me", User.class);
				LOGGER.debug(user);

				Connection<Conversation> conversations = facebookClient
						.fetchConnection("me/Conversations", Conversation.class);

				String batchRunSuccess = "N";
				
				for (List<Conversation> conversationPage : conversations) {
					for (Conversation conversation : conversationPage) {
						//LOGGER.debug(conversation);
						//LOGGER.debug(conversation.getUnreadCount());
						String conversationId = conversation.getId();
						/*LOGGER.debug("Conversation id :"
								+ conversationId);*/
						StringBuilder finalMessage = new StringBuilder();
						String recipient = "";
						String receipantName="";
						Stack<String> messageStack = new Stack<String>();
						for (Message message : conversation.getMessages()) {
							String messageId = message.getId();
							//LOGGER.debug("Message id :" + messageId);
							if (fbMessageDAO.getMessageCount(conversationId, messageId) ==0 && !message.getTags().contains("sent")) {
								LOGGER.debug("Tags :"+message.getTags());
								LOGGER.debug("Requesting processing starts for Message Id =" + messageId +",conversation Id =" +conversationId);
								LOGGER.debug("Message text = "
										+ message.getMessage());
								LOGGER.debug("Message unread = "
										+ message.getUnread());
								LOGGER.debug("Message from = "
										+ message.getFrom().getName());
								LOGGER.debug("Message to = "
										+ message.getTo().get(0).getName());
								LOGGER.debug("Message unseen = "
										+ message.getUnseen());
								// lastMessage = message;
								recipient = message.getFrom().getId();
								receipantName = message.getFrom().getName();

								LOGGER.debug("From ID -" + recipient);
								
								messageStack.push(message.getMessage());
								
								fbMessageDAO.insert(conversationId, messageId, message.getFrom().getId(), message.getTo().get(0).getId(), message.getFrom().getName(), message.getTo().get(0).getName(), new Timestamp(System.currentTimeMillis()));
								 
							}

						}
						if(!messageStack.isEmpty()){
							while(!messageStack.isEmpty()){
								finalMessage.append(messageStack.pop()).append(" ");
							}
							IdMessageRecipient recipient1 = new IdMessageRecipient(recipient);
							String receivedMessage =  "";
							boolean isResponseSuccess = false;
							try{	
								receivedMessage =  getReply(finalMessage.toString(),conversationId,receipantName);
								isResponseSuccess = true;
							}catch(NMPException ne){
								LOGGER.debug(ne.getMessage());
							} 
							if(!isResponseSuccess){
								String commonMessage = "Hi "+ receipantName +", We are unable to process yor request right now.Our Call Center Team will contact you shortly";
								SendResponse resp =facebookClient.publish(conversation.getId() +"/messages",SendResponse.class,Parameter.with("recipient",
										recipient1),Parameter.with("message", commonMessage));
								LOGGER.debug(resp.toString());
								batchRunSuccess = "N";
								continue;
							}else{							
								
								SendResponse resp =facebookClient.publish(conversation.getId() +"/messages",SendResponse.class,Parameter.with("recipient",
										recipient1),Parameter.with("message", receivedMessage));
								LOGGER.debug(resp.toString());
								batchRunSuccess = "Y";
							}
						}
					}
				}
				batchResultDAO.insert(new Timestamp(System.currentTimeMillis()), batchRunSuccess);

			}catch(Exception e){
				LOGGER.debug("System Error");
				LOGGER.error(e);
			}
			
			


	}

	@Override
	public void send() throws MalformedURLException, IOException {
		LOGGER.debug("Call thorugh batch...");
		batchResultDAO.insert(new Timestamp(System.currentTimeMillis()), "Y");

	}

	

	/*public static void main(String args[]) throws IOException {
		
		 * FBMessageSenderReceiverImpl fbm = new FBMessageSenderReceiverImpl();
		 * fbm.receiver();
		 
		String tempToken = "EAAOPIkINIzUBAB8OZARTxFft3XpKV5cfZCk0meJXXzlN8L0vnWJkEGIpHqqNIvSqycmwDkXVWZAnpJxL5syd8xepph0RCZAz5UO5pbEAYZCaoWijLK6000MIxh4bwZBZAvLrWFp0WBNV0oneFfbvvV7cZBPg6LmJqYLjj7qUjS460w0hLzQunY4JinATJoZAZC0QcZD";

		FacebookClient facebookClient = new DefaultFacebookClient(tempToken, Version.UNVERSIONED);
		AccessToken extendedAccessToken = facebookClient.obtainExtendedAccessToken("dasd", "asdasd", tempToken);
		System.out.println("Extended access token : " + extendedAccessToken.getAccessToken());;
	}*/

}
