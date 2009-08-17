package org.mule.transport.http.components;

import java.io.ByteArrayInputStream;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

/**
 * A simple component which returns a stream.
 * @author Dan
 *
 */
public class StreamingResponseComponent implements Callable {

	public Object onCall(MuleEventContext eventContext) throws Exception {
		return new ByteArrayInputStream("hello".getBytes());
	}

}
