/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.exception;

import org.mule.api.MuleEvent;
import org.mule.api.exception.MessagingExceptionHandler;

public class MessagingExceptionHandlerToSystemAdapter implements MessagingExceptionHandler
{

    @Override
    public MuleEvent handleException(Exception exception, MuleEvent event)
    {
        event.getMuleContext().getExceptionListener().handleException(exception);
        return event;
    }
}
