/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp;

import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.concurrent.Latch;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jivesoftware.smack.packet.Message;
import org.junit.Test;

public class XmppDynamicEndpointTestCase extends FunctionalTestCase
{
    private Latch latch = new Latch();

    @Override
    protected boolean isDisabledInThisEnvironment()
    {
        return XmppEnableDisableTestCase.isTestDisabled();
    }

    @Override
    protected String[] getConfigFiles()
    {
        return new String[] {
            AbstractXmppTestCase.COMMON_CONFIG,
            "xmpp-dynamic-endpoint-config.xml"
        };
    }

    @Test
    public void testDispatchChat() throws Exception
    {
        configureTestComponent();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("recipient", "mule2@localhost");
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, properties, muleContext);

        MuleClient client = muleContext.getClient();
        client.dispatch("vm://fromTest", message, properties);

        assertTrue(latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    private void configureTestComponent() throws Exception
    {
        EventCallback callback = new XmppCallback(latch, Message.Type.chat);

        Object component = getComponent("receive");
        assertTrue(component instanceof FunctionalTestComponent);

        FunctionalTestComponent testComponent = (FunctionalTestComponent) component;
        testComponent.setEventCallback(callback);
    }
}


