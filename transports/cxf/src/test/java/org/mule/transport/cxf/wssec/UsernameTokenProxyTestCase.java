/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.wssec;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

import java.io.InputStream;

public class UsernameTokenProxyTestCase extends FunctionalTestCase {
	public void testProxyEnvelope() throws Exception 
	{
		MuleMessage result = sendRequest("http://localhost:63081/proxy-envelope");

		System.out.println(result.getPayloadAsString());
		assertFalse(result.getPayloadAsString().contains("Fault"));
//		assertFalse(result.getPayloadAsString().contains("ross"));
		assertTrue(result.getPayloadAsString().contains("User001"));
	}
	
	public void testProxyBody() throws Exception 
	{
		MuleMessage result = sendRequest("http://localhost:63081/proxy-body");

		System.out.println(result.getPayloadAsString());
		assertFalse(result.getPayloadAsString().contains("Fault"));
		assertFalse(result.getPayloadAsString().contains("ross"));
		assertFalse(result.getPayloadAsString().contains("User001"));
	}


	private MuleMessage sendRequest(String url) throws MuleException 
	{
		ClientPasswordCallback.setPassword("test");
		
		MuleClient client = new MuleClient();

		InputStream stream = getClass().getResourceAsStream(
				"/org/mule/transport/cxf/wssec/in-message.xml");
		assertNotNull(stream);

		MuleMessage result = client.send(url, new DefaultMuleMessage(stream, muleContext));
		return result;
	}
	
	protected String getConfigResources() 
	{
		return "org/mule/transport/cxf/wssec/username-token-proxy-conf.xml";
	}

}
