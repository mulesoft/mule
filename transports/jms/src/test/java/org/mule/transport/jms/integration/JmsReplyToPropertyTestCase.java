/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.integration;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JmsReplyToPropertyTestCase extends AbstractJmsFunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "integration/jms-replyto-property.xml";
    }

    @Test
    public void testReplyTo() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map<String, String> props = new HashMap<String, String>();
        props.put("JMSReplyTo", "middle");
        client.dispatch("in", DEFAULT_INPUT_MESSAGE, props);

        // Check that the property is still on the outbound message
        MuleMessage output = client.request("out", 2000);
        assertNotNull(output);
        final Object o = output.getOutboundProperty("JMSReplyTo");
        assertTrue(o.toString().contains("middle"));
        
        // Check that the reply message was generated
        output = client.request("middle", 2000);
        assertNotNull(output);
        assertEquals(DEFAULT_OUTPUT_MESSAGE, output.getPayload());
    }
}
