/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    protected String getConfigResources()
    {
        return AbstractXmppTestCase.COMMON_CONFIG + ", xmpp-dynamic-endpoint-config.xml";
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


