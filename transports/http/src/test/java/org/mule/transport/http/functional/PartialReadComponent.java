package org.mule.transport.http.functional;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

import java.io.InputStream;

/**
 * 
 */
public class PartialReadComponent implements Callable {

	public Object onCall(MuleEventContext eventContext) throws Exception {
		InputStream stream = (InputStream) eventContext.getMessage().getPayload(InputStream.class);
		
		stream.read();
		return "Hello";
	}

}
