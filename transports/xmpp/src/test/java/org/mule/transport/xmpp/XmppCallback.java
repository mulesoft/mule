/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.concurrent.Latch;

import junit.framework.Assert;
import org.jivesoftware.smack.packet.Message;

import static org.junit.Assert.assertEquals;

public class XmppCallback implements EventCallback
{
    private Latch latch;
    private Message.Type expectedMessageType;

    public XmppCallback(Latch latch, Message.Type type)
    {
        super();
        this.latch = latch;
        this.expectedMessageType = type;
    }

    public void eventReceived(MuleEventContext context, Object component) throws Exception
    {
        MuleMessage muleMessage = context.getMessage();
        Object payload = muleMessage.getPayload();
        Assert.assertTrue(payload instanceof Message);
        
        Message xmppMessage = (Message) payload;
        assertEquals(expectedMessageType, xmppMessage.getType());
        assertEquals(AbstractMuleContextTestCase.TEST_MESSAGE, xmppMessage.getBody());
        
        latch.countDown();
    }
}

