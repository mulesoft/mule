/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.message;

import static org.junit.Assert.assertEquals;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Ignore;
import org.junit.Test;


/**
 * see EE-1794
 */
@Ignore("BL-38 Need to port for CXF changes")
public class MessagePropertyInResponseTransformersTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/messaging/message-property-in-response-transformers.xml";
    }

    @Test
    public void testSend() throws Exception
    {
        MuleClient client = muleContext.getClient();
        final String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:sim=\"http://simple.component.mule.org/\"><soapenv:Header/><soapenv:Body><sim:echo><sim:echo>aaa</sim:echo></sim:echo></soapenv:Body></soapenv:Envelope>";
        MuleMessage result = client.send("http://localhost:63081/ser", new DefaultMuleMessage(payload, muleContext));
        assertEquals(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root xmlns=\"http://simple.component.mule.org/\"><testval>bar</testval></root>",
            result.getPayloadAsString());
    }
}
