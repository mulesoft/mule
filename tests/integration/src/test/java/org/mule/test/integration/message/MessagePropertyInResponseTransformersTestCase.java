/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.message;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * see EE-1794
 */
public class MessagePropertyInResponseTransformersTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/messaging/message-property-in-response-transformers.xml";
    }

    @Test
    public void testSend() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("http://localhost:63081/ser",
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:sim=\"http://simple.component.mule.org/\"><soapenv:Header/><soapenv:Body><sim:echo><sim:echo>aaa</sim:echo></sim:echo></soapenv:Body></soapenv:Envelope>", null);
        assertEquals(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root xmlns=\"http://simple.component.mule.org/\"><testval>bar</testval></root>",
            result.getPayloadAsString());
    }
}
