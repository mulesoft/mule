/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.exception;

import org.mule.api.MuleContext;

/**
 * This is the default exception handler for any exception which does not inherit from MessagingException, 
 * i.e, when no message is in play.  The exception handler will fire a notification, log exception, 
 * roll back any transaction, and trigger a reconnection strategy if this is a <code>ConnectException</code>.
 */
public class DefaultSystemExceptionStrategy extends AbstractSystemExceptionStrategy
{
    public DefaultSystemExceptionStrategy(MuleContext muleContext)
    {
        super(muleContext);
    }
}
