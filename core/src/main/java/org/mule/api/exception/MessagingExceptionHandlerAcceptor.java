/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.exception;

import org.mule.api.Acceptor;

/**
 *  MessagingExceptionHandlers that will only be executed if accepts to manage MuleEvent
 */
public interface MessagingExceptionHandlerAcceptor extends MessagingExceptionHandler, Acceptor
{
}
