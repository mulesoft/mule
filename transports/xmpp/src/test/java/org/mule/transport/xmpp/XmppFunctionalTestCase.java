/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.transport.NullPayload;

public class XmppFunctionalTestCase extends XmppEnableDisableTestCase
{
    
    public XmppFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, AbstractXmppTestCase.COMMON_CONFIG + "," + "xmpp-functional-config-service.xml"},
            {ConfigVariant.FLOW, AbstractXmppTestCase.COMMON_CONFIG + "," + "xmpp-functional-config-flow.xml"}
        });
    }

//    @Test
//    public void testDispatchNormalMessage() throws Exception
//    {
//        MuleClient client = new MuleClient(muleContext);
//        Map<String, String> messageProperties = new HashMap<String, String>();
//        messageProperties.put(XmppConnector.XMPP_SUBJECT, "da subject");
//        client.dispatch("vm://in", TEST_MESSAGE, messageProperties);
//        
//        Thread.sleep(10000);
//    }
    
//    @Test
//    public void testSendNormalMessage() throws Exception
//    {
//        MuleClient client = new MuleClient(muleContext);
//        MuleMessage result = client.send("vm://in", TEST_MESSAGE, null);
//        assertNotNull(result);
//        assertFalse(result.getPayload() instanceof NullPayload);
//    }

//    @Test
//    public void testDispatchChat() throws Exception
//    {
//        MuleClient client = new MuleClient(muleContext);
//        client.dispatch("vm://in", TEST_MESSAGE, null);
//
//        Thread.sleep(10000);
//    }

    @Test
    public void testSendChat() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage result = client.send("vm://in", TEST_MESSAGE, null);
        assertNotNull(result);
        assertFalse(result.getPayload() instanceof NullPayload);
    }
}
