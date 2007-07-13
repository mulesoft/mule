/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.functional;

import org.mule.config.i18n.MessageFactory;
import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.InitialisationException;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

import junit.framework.AssertionFailedError;

public class CountdownCallback implements EventCallback
{
    private CountDownLatch countDown;

    public CountdownCallback(int messagesExpected)
    {
        this.countDown = new CountDownLatch(messagesExpected);
    }

    public void eventReceived(UMOEventContext context, Object Component) throws Exception
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