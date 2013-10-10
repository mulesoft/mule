/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.xmpp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.transport.NullPayload;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

@Ignore
public class XmppFunctionalTestCase extends XmppEnableDisableTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
            {ConfigVariant.SERVICE, "xmpp-functional-config-service.xml"},
            {ConfigVariant.FLOW, "xmpp-functional-config-flow.xml"}
        });
    }

    public XmppFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testDispatchNormalMessage() throws Exception
    {
        MuleClient client = muleContext.getClient();
        Map<String, Object> messageProperties = new HashMap<String, Object>();
        messageProperties.put(XmppConnector.XMPP_SUBJECT, "da subject");
        client.dispatch("vm://in", TEST_MESSAGE, messageProperties);

        Thread.sleep(10000);
    }

    @Test
    public void testSendNormalMessage() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("vm://in", TEST_MESSAGE, null);
        assertNotNull(result);
        assertFalse(result.getPayload() instanceof NullPayload);
    }

    @Test
    public void testDispatchChat() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://in", TEST_MESSAGE, null);

        Thread.sleep(10000);
    }

    @Test
    public void testSendChat() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("vm://in", TEST_MESSAGE, null);
        assertNotNull(result);
        assertFalse(result.getPayload() instanceof NullPayload);
    }
}
