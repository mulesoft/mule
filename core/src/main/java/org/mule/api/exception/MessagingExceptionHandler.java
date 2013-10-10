/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.exception;

import org.mule.api.MuleEvent;

/**
 * Take some action when a messaging exception has occurred (i.e., there was a message in play when the exception occurred).
 */
public interface MessagingExceptionHandler extends ExceptionHandler
{
    /**
     * Take some action when a messaging exception has occurred (i.e., there was a message in play when the exception occurred).
     * 
     * @param exception which occurred
     * @param event which was being processed when the exception occurred
     * @return new event to route on to the rest of the flow, generally with ExceptionPayload set on the message
     */
    MuleEvent handleException(Exception exception, MuleEvent event);
}


