/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
