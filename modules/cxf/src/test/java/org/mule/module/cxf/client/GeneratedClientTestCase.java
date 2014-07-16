/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.client;

import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class GeneratedClientTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "proxy-conf-service.xml";
    }

    @Test
    public void testEchoService() throws Exception
    {
        // URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        // assertNotNull(wsdl);
        // SOAPService service = new SOAPService(wsdl, null);
        // Greeter soapPort = service.getSoapPort();
        //
        String msg = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                     + "<soap:Body>" + "<test> foo </test>" + "</soap:Body>" + "</soap:Envelope>";

        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/Echo", msg, null);
        byte[] res = (byte[]) result.getPayload();
        String resString = new String(res);

        assertTrue(resString.indexOf("<test> foo </test>") != -1);
    }
}
