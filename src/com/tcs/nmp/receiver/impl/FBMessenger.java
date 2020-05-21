package com.tcs.nmp.receiver.impl;

import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.Version;
import com.restfb.types.Conversation;
import com.restfb.types.Message;
import com.restfb.types.User;
import com.restfb.types.send.SendResponse;

public class FBMessenger {

	static String accessToken = "EAACEdEose0cBAICCAZBvx7ABfOBSK14ZCb2RpZBGO0pxi9AqnFyTrao0ZCQSm0ZBhjQrZAgwhlZBT5mqrGyAVF91FxjG8hxJYugRcoZC7k5uiPeNLcb63pGtQ0VyfRFpfDcyfFsEE8bzNYmzFWcXA2Ff5ZCZBIXUBK7szbCXjWLnJ5wRgSW55rzjNg";

	//private final static String USER_AGENT = "Mozilla/5.0";
	
	private static final Logger LOGGER =  LogManager.getLogger(FBMessenger.class);
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return new java.security.cert.X509Certificate[0];
				}

				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}

				public void checkServerTrusted(X509Certificate[] certs, String authType) {
				}
			} };
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

			// Create all-trusting host name verifier
			HostnameVerifier allHostsValid = new HostnameVerifier() {
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			};
			// Install the all-trusting host verifier
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
			/* End of the fix */
			// System.setProperty("javax.net.ssl.trustStore","C:/Shibu/Work/MetLife/Software/jdk1.6.0_37/jdk1.6.0_37/jre/lib/security/cacerts");
			// System.setProperty("javax.net.ssl.keyStorePassword", "changeit");
			FacebookClient facebookClient = new DefaultFacebookClient(accessToken, Version.UNVERSIONED);
			User user = (User) facebookClient.fetchObject("me", User.class);
			LOGGER.debug(user);

			Connection<Conversation> conversations = facebookClient.fetchConnection("me/Conversations", Conversation.class);

			for (List<Conversation> conversationPage : conversations) {
				for (Conversation conversation : conversationPage) {
					LOGGER.debug(conversation);
					LOGGER.debug(conversation.getUnreadCount());

					// Message lastMessage = null;
					for (Message message : conversation.getMessages()) {
						LOGGER.debug("Message text = " + message.getMessage());
						LOGGER.debug("Message unread = " + message.getUnread());
						LOGGER.debug("Message from = " + message.getFrom().getName());
						LOGGER.debug("Message to = " + message.getTo().get(0).getName());
						LOGGER.debug("Message unseen = " + message.getUnseen());
						// lastMessage = message;
						String recipient = message.getFrom().getId();

						LOGGER.debug("From ID -" + recipient);
						
						
						
						
						//sendng back to user
						
						Message simpleTextMessage = new Message();
						simpleTextMessage.setMessage("Response");
						SendResponse resp = facebookClient.publish(conversation.getId() + "/messages", SendResponse.class,Parameter.with("recipient", recipient),Parameter.with("message", simpleTextMessage)); 
						LOGGER.debug(resp.isSuccessful());

					}
				}
			}

			// LOGGER.debug(pages);

			/*
			 * String USER_AGENT = "Mozilla/5.0"; URL url = new
			 * URL("https://developers.facebook.com"); HttpsURLConnection
			 * connection = (HttpsURLConnection)url.openConnection();
			 * connection.setRequestMethod("GET");
			 * connection.setRequestProperty("User-Agent", USER_AGENT); int
			 * responseCode = connection.getResponseCode();
			 * LOGGER.debug("responseCode=" + responseCode);
			 */

		} catch (Exception e) {
			LOGGER.error(e);
		}
	}

}

