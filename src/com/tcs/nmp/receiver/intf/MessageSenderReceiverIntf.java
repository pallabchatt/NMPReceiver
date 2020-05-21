package com.tcs.nmp.receiver.intf;

import java.io.IOException;
import java.net.MalformedURLException;

public interface MessageSenderReceiverIntf {
	
	public void send() throws MalformedURLException, IOException;
	
	public void receive();

	String getReply(String input,String conversationId,String name) throws IOException;

}
