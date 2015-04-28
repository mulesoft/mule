/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import org.mule.api.CompletionHandler;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.processor.AbstractRequestResponseMessageProcessor;
import org.mule.util.concurrent.Latch;

/**
 * Can be used to sense request and response threads used during processing.
 */
public class SensingNullRequestResponseMessageProcessor extends AbstractRequestResponseMessageProcessor
{
    public Thread requestThread;
    public Thread responseThread;

    @Override
    protected MuleEvent processRequest(MuleEvent event) throws MuleException
    {
        requestThread = Thread.currentThread();
        return super.processRequest(event);
    }

    @Override
    protected MuleEvent processResponse(MuleEvent event) throws MuleException
    {
        responseThread = Thread.currentThread();
        return super.processRequest(event);
    }

}
