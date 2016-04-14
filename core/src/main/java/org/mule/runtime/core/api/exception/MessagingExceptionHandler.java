/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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


