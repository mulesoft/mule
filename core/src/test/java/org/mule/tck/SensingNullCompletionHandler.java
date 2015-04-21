/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.CompletionHandler;
import org.mule.util.concurrent.Latch;

public class SensingNullCompletionHandler implements CompletionHandler<MuleEvent, MessagingException>
{
    public MuleEvent event;
    public Exception exception;
    public Latch latch = new Latch();

    @Override
    public void onCompletion(MuleEvent event)
    {
        this.event = event;
        latch.countDown();
    }

    @Override
    public void onFailure(MessagingException exception)
    {
        this.exception = exception;
        latch.countDown();
    }

    public void clear()
    {
        event = null;
    }

}
