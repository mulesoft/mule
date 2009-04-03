/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.integration;

import javax.jms.Message;

import org.junit.Test;

public class JmsDurableTopicTestCase extends AbstractJmsFunctionalTestCase
{
    public JmsDurableTopicTestCase(JmsVendorConfiguration config)
    {
        super(config);
        setUseTopics(true);
    }

    protected String getConfigResources()
    {
        return "integration/jms-durable-topic.xml";
    }

    @Test
    public void testProviderDurableSubscriber() throws Exception
    {
        setClientId("Client1");
        assertNull(receiveNoWait());
        setClientId("Client2");
        assertNull(receiveNoWait());

        setClientId("Sender");
        send();

        setClientId("Client1");
        Message output = receive();
        assertPayloadEquals(DEFAULT_INPUT_MESSAGE, output);
        assertNull(receiveNoWait());
        setClientId("Client2");
        output = receive();
        assertPayloadEquals(DEFAULT_INPUT_MESSAGE, output);
        assertNull(receiveNoWait());
    }
}
