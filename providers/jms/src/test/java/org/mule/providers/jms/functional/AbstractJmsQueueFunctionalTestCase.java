/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.jms.functional;

import EDU.oswego.cs.dl.util.concurrent.CountDown;
import org.mule.MuleManager;
import org.mule.providers.jms.support.JmsTestUtils;
import org.mule.tck.functional.EventCallback;
import org.mule.umo.UMOEventContext;

import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.QueueConnection;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.TopicConnection;

/**
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public abstract class AbstractJmsQueueFunctionalTestCase extends AbstractJmsFunctionalTestCase
{
    public void testSend() throws Exception
    {
        final CountDown countDown = new CountDown(2);

        EventCallback callback = new EventCallback()
        {
            public void eventReceived(UMOEventContext context, Object Component)
            {
                callbackCalled = true;
                assertNull(context.getCurrentTransaction());
                countDown.release();
            }
        };

        initialiseComponent(callback);
        //Start the server
        MuleManager.getInstance().start();

        MessageConsumer mc;
        //check replyTo
        if(useTopics()) {
            mc = JmsTestUtils.getTopicSubscriber((TopicConnection)cnn, getOutDest().getAddress());
        } else {
            mc = JmsTestUtils.getQueueReceiver((QueueConnection)cnn, getOutDest().getAddress());
        }
        mc.setMessageListener(new MessageListener(){
            public void onMessage(Message message)
            {
                currentMsg = message;
                countDown.release();
            }
        });
        afterInitialise();
        send(DEFAULT_MESSAGE, false, Session.AUTO_ACKNOWLEDGE, null);
        assertTrue(countDown.attempt(LOCK_WAIT));

        assertNotNull(currentMsg);
        assertTrue(currentMsg instanceof TextMessage);
        assertEquals(DEFAULT_MESSAGE + " Received", ((TextMessage) currentMsg).getText());

        assertTrue(callbackCalled);
    }


    public void testSendWithReplyTo() throws Exception
    {
        final CountDown countDown = new CountDown(2);
        EventCallback callback = new EventCallback()
        {
            public void eventReceived(UMOEventContext context, Object Component)
            {
                callbackCalled = true;
                assertNull(context.getCurrentTransaction());
                countDown.release();
            }
        };

        initialiseComponent(callback);
        //Start the server
        MuleManager.getInstance().start();

        Message msg = null;

        MessageConsumer mc;
        //check replyTo
        if(useTopics()) {
            mc = JmsTestUtils.getTopicSubscriber((TopicConnection)cnn, "replyto");
        } else {
            mc = JmsTestUtils.getQueueReceiver((QueueConnection)cnn, "replyto");
        }
        mc.setMessageListener(new MessageListener(){
            public void onMessage(Message message)
            {
                currentMsg = message;
                countDown.release();
            }
        });

        send(DEFAULT_MESSAGE, false, Session.AUTO_ACKNOWLEDGE, "replyto");
        afterInitialise();

        assertTrue(countDown.attempt(LOCK_WAIT));

        assertNotNull(currentMsg);
        assertTrue(currentMsg instanceof TextMessage);
        assertEquals(DEFAULT_MESSAGE + " Received", ((TextMessage) currentMsg).getText());
        assertTrue(callbackCalled);
    }

    public boolean useTopics() {
        return false;
    }
}
