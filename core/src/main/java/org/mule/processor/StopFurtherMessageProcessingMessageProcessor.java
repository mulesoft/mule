/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.processor.AbstractInterceptingMessageProcessor;

public class StopFurtherMessageProcessingMessageProcessor extends AbstractInterceptingMessageProcessor
{
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (!event.isStopFurtherProcessing())
        {
            return processNext(event);
        }
        else
        {
            return event;
        }
    }
}
