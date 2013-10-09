/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.integration;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.transport.NullPayload;
import org.mule.transport.jms.JmsConstants;
import org.mule.util.StringUtils;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * see EE-1688/MULE-3059
 */
public class JmsSynchronousResponseTestCase extends AbstractJmsFunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "integration/jms-synchronous-response.xml";
    }

    @Test
    public void testResponseWithoutReplyTo() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        MuleMessage response = client.send("out1", "TEST_MESSAGE", null);
        assertNotNull(response);
        assertTrue("Response is not a JMS Message", response.getPayload() instanceof javax.jms.Message);
        assertJmsMessageIdPresent(response);
    }

    @Test
    public void testResponseWithoutReplyToEndpointProperties() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        MuleMessage response = client.send("out2", "TEST_MESSAGE", null);
        assertNotNull(response);
        assertTrue("Response is not a JMS Message", response.getPayload() instanceof javax.jms.Message);
        assertJmsMessageIdPresent(response);
    }

    @Test
    public void testResponseWithReplyTo() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        MuleMessage response = client.send("out3", "TEST_MESSAGE", null);
        assertNotNull(response);
        assertFalse("Response should not be NullPayload", response.getPayload() instanceof NullPayload);
    }

    private void assertJmsMessageIdPresent(MuleMessage message)
    {
        String messageId = message.getInboundProperty(JmsConstants.JMS_MESSAGE_ID);
        assertTrue("JMSMessageID is missing", StringUtils.isNotBlank(messageId));
    }
}
