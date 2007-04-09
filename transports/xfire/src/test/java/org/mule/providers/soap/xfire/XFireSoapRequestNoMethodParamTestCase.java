/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire;

import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class XFireSoapRequestNoMethodParamTestCase extends FunctionalTestCase
{
    private static final String request = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><soap:Body><receive xmlns=\"http://www.muleumo.org\"><src xmlns=\"http://www.muleumo.org\">Test String</src></receive></soap:Body></soap:Envelope>";
    private static final String response = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><soap:Body><receiveResponse xmlns=\"http://www.muleumo.org\"><out xmlns=\"http://www.muleumo.org\">Received: Test String</out></receiveResponse></soap:Body></soap:Envelope>";

    public void testXFireSoapRequest() throws Exception
    {
        MuleClient client = new MuleClient();

        UMOMessage msg = client.send("http://localhost:33381/services/TestComponent",
            new MuleMessage(request));

        assertNotNull(msg);
        assertNotNull(msg.getPayload());
        System.out.println("1: " + response);
        System.out.println("2: " + msg.getPayloadAsString());
        assertEquals(response, msg.getPayloadAsString());
    }

    protected String getConfigResources()
    {
        return "xfire-soap-request-conf.xml";
    }

}
