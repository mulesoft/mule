/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.soap.xfire;

import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class XFireSoapRequestNoMethodParamTestCase extends FunctionalTestCase{

	public void testXfireSoapRequest() throws Exception
	{
		MuleClient client = new MuleClient();
		
		UMOMessage msg = client.send("http://localhost:33381/services/TestComponent", new MuleMessage("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><soap:Body><receive xmlns=\"http://www.muleumo.org\"><src xmlns=\"http://www.muleumo.org\">Test String</src></receive></soap:Body></soap:Envelope>"));
		
		assertNotNull(msg);
		assertNotNull(msg.getPayload());
		assertEquals(msg.getPayloadAsString(),"<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><soap:Body><receiveResponse xmlns=\"http://www.muleumo.org\"><out>Received: Test String</out></receiveResponse></soap:Body></soap:Envelope>");
	}
	
	protected String getConfigResources() {
		return "org/mule/test/integration/providers/soap/xfire/mule-xfire-soap-request.xml";
	}

}
