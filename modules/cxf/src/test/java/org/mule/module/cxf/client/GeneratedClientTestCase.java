/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.client;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class GeneratedClientTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
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

        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/Echo", msg, null);
        byte[] res = (byte[]) result.getPayload();
        String resString = new String(res);

        assertTrue(resString.indexOf("<test> foo </test>") != -1);
    }
}
