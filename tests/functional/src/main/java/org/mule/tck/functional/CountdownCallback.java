/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.functional;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.MessageFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.AssertionFailedError;

public class CountdownCallback implements EventCallback
{
    private CountDownLatch countDown;

    public CountdownCallback(int messagesExpected)
    {
        this.countDown = new CountDownLatch(messagesExpected);
    }

    public void eventReceived(MuleEventContext context, Object Component) throws Exception
    {
        synchronized (this)
        {            
            if (countDown.getCount() > 0)
            {
                countDown.countDown();
            }
            else
            {
                throw new AssertionFailedError("Too many messages received");
            }
        }
    }

    public long getCount() throws InitialisationException
    {
        if (countDown != null)
        {
            return countDown.getCount();
        }
        else 
        {
            throw new InitialisationException(MessageFactory.createStaticMessage("CountDownLatch has not been initialized."), null);
        }
    }

    public boolean await(long timeout) throws InterruptedException
    {
        return countDown.await(timeout, TimeUnit.MILLISECONDS);
    }
    
}
