/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class SoapRequestNoMethodParamTestCase extends FunctionalTestCase
{
    private static final String request = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><soap:Body><receive xmlns=\"http://www.muleumo.org\"><src xmlns=\"http://www.muleumo.org\">Test String</src></receive></soap:Body></soap:Envelope>";
    private static final String response = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns1:receiveResponse xmlns:ns1=\"http://services.testmodels.tck.mule.org/\"><ns1:return>Received: null</ns1:return></ns1:receiveResponse></soap:Body></soap:Envelope>";

    public void testCXFSoapRequest() throws Exception
    {
        MuleClient client = new MuleClient();

        MuleMessage msg = client.send("http://localhost:63381/services/TestComponent",
            new DefaultMuleMessage(request));

        assertNotNull(msg);
        assertNotNull(msg.getPayload());
        assertEquals(response, msg.getPayloadAsString());
    }

    protected String getConfigResources()
    {
        return "soap-request-conf.xml";
    }

}
