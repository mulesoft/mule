/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.functional;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.module.cxf.example.HelloWorld;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Map;

import javax.jws.WebService;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class UnwrapsComponentExceptionTestCase extends FunctionalTestCase
{

    public static final String ERROR_MESSAGE = "Changos!!!";

    private static final String requestPayload =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "           xmlns:hi=\"http://example.cxf.module.mule.org/\">\n" +
            "<soap:Body>\n" +
            "<hi:sayHi>\n" +
            "    <arg0>Hello</arg0>\n" +
            "</hi:sayHi>\n" +
            "</soap:Body>\n" +
            "</soap:Envelope>";

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "unwraps-component-exception-config.xml";
    }

    @Test
    public void testReceivesComponentExceptionMessage() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(requestPayload, (Map<String, Object>) null, muleContext);
        LocalMuleClient client = muleContext.getClient();
        MuleMessage received = client.send("http://localhost:" + dynamicPort.getNumber() + "/hello", request);

        assertTrue("Component exception was not managed", received.getPayloadAsString().contains(ERROR_MESSAGE));
    }

    @WebService(endpointInterface = "org.mule.module.cxf.example.HelloWorld", serviceName = "HelloWorld")
    public static class HelloWorldImpl implements HelloWorld
    {

        public String sayHi(String text)
        {
            throw new RuntimeException(ERROR_MESSAGE);
        }
    }
}
