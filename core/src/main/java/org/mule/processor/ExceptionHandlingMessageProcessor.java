/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.config.ExceptionHelper;
import org.mule.message.DefaultExceptionPayload;
import org.mule.transport.NullPayload;

public class ExceptionHandlingMessageProcessor extends AbstractInterceptingMessageProcessor
{
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        try
        {
            return processNext(event);
        }
        catch (Exception e)
        {
            e = (Exception) ExceptionHelper.sanitizeIfNeeded(e);
            return event.getFlowConstruct().getExceptionListener().handleException(e, event);
        }
    }
}
