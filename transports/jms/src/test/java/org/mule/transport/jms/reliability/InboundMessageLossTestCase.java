/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.reliability;

import org.mule.api.context.notification.ExceptionNotificationListener;
import org.mule.api.model.Model;
import org.mule.context.notification.ExceptionNotification;
import org.mule.exception.DefaultServiceExceptionStrategy;
import org.mule.exception.DefaultSystemExceptionStrategy;
import org.mule.transport.jms.redelivery.MessageRedeliveredException;
import org.mule.util.concurrent.Latch;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class InboundMessageLossTestCase extends AbstractJmsReliabilityTestCase
{
    private Latch messageRedelivered;
    private final int latchTimeout = getTestTimeoutSecs() * 1000 / 4;
    
    @Override
    protected String getConfigResources()
    {
        return "reliability/activemq-config.xml, reliability/inbound-message-loss.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        
        // Set SystemExceptionStrategy to redeliver messages
        ((DefaultSystemExceptionStrategy) muleContext.getExceptionListener()).setRedeliver(true);
        
        // Set ServiceExceptionStrategy to redeliver messages
        Model model = muleContext.getRegistry().lookupModel("default");
        ((DefaultServiceExceptionStrategy) model.getExceptionListener()).setRedeliver(true);
        
        // Tell us when a MessageRedeliverdException has been handled
        messageRedelivered = new Latch();
        muleContext.registerListener(new ExceptionNotificationListener<ExceptionNotification>()
        {
            public void onNotification(ExceptionNotification notification)
            {
                if (notification.getException() instanceof MessageRedeliveredException)
                {
                    messageRedelivered.countDown();
                }
            }
        });
    }

    public void testNoException() throws Exception
    {
        putMessageOnQueue("noException");

        // Delivery was successful 
        messageRedelivered.await(latchTimeout, TimeUnit.MILLISECONDS);
        assertEquals("Message should not have been redelivered", 1, messageRedelivered.getCount());
    }
    
    public void testTransformerException() throws Exception
    {
        putMessageOnQueue("transformerException");

        // Delivery failed so message should have been redelivered
        messageRedelivered.await(latchTimeout, TimeUnit.MILLISECONDS);
        assertEquals("Message was not redelivered", 0, messageRedelivered.getCount());
    }
    
    public void testRouterException() throws Exception
    {
        putMessageOnQueue("routerException");

        // Delivery failed so message should have been redelivered
        messageRedelivered.await(latchTimeout, TimeUnit.MILLISECONDS);
        assertEquals("Message was not redelivered", 0, messageRedelivered.getCount());
    }
    
    public void testComponentException() throws Exception
    {
        putMessageOnQueue("componentException");
        
        // A component exception occurs after the SEDA queue, so message should not have been redelivered
        messageRedelivered.await(latchTimeout, TimeUnit.MILLISECONDS);
        assertEquals("Message should not have been redelivered", 1, messageRedelivered.getCount());
    }    
}


