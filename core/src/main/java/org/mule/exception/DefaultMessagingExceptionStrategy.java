/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.exception;

import org.mule.api.MuleContext;

/**
 * This is the default exception handler for flows and services. The handler logs errors 
 * and will forward the message and exception to an exception endpoint if one is set 
 * on this Exception strategy.  If an endpoint is configured via the <default-exception-strategy> 
 * element, a Dead Letter Queue pattern is assumed and so the transaction will commit.
 * Otherwise, the transaction will rollback, possibly causing the source message to be 
 * redelivered (depends on the transport).
 */
public class DefaultMessagingExceptionStrategy extends AbstractMessagingExceptionStrategy
{
    public DefaultMessagingExceptionStrategy()
    {
    }

    public DefaultMessagingExceptionStrategy(MuleContext muleContext)
    {
        super(muleContext);
    }
}
