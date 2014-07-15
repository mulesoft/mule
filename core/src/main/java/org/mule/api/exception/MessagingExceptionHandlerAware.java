/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.exception;

/**
 * Objects that need access to the messaging exception handler of the execution context should implement this interface.
 */
public interface MessagingExceptionHandlerAware
{
    void setMessagingExceptionHandler(MessagingExceptionHandler messagingExceptionHandler);
}
