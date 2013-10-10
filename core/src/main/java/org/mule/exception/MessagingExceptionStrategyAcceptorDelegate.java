/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.exception;

import java.util.Arrays;
import java.util.List;

import org.mule.api.MuleEvent;
import org.mule.api.exception.MessagingExceptionHandlerAcceptor;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.processor.AbstractMuleObjectOwner;

/**
 * Allows to use {@link org.mule.api.exception.MessagingExceptionHandler} as {@link org.mule.api.exception.MessagingExceptionHandlerAcceptor}.
 */
public class MessagingExceptionStrategyAcceptorDelegate extends AbstractMuleObjectOwner<MessagingExceptionHandler> implements MessagingExceptionHandlerAcceptor
{
    private MessagingExceptionHandler delegate;

    public MessagingExceptionStrategyAcceptorDelegate(MessagingExceptionHandler messagingExceptionHandler)
    {
        this.delegate = messagingExceptionHandler;
    }

    @Override
    public boolean accept(MuleEvent event)
    {
        if (delegate instanceof MessagingExceptionHandlerAcceptor)
        {
            return ((MessagingExceptionHandlerAcceptor)delegate).accept(event);
        }
        return true;
    }

    @Override
    public boolean acceptsAll()
    {
        if (delegate instanceof MessagingExceptionHandlerAcceptor)
        {
            return ((MessagingExceptionHandlerAcceptor)delegate).acceptsAll();
        }
        return true;
    }

    @Override
    public MuleEvent handleException(Exception exception, MuleEvent event)
    {
        return delegate.handleException(exception,event);
    }

    @Override
    protected List<MessagingExceptionHandler> getOwnedObjects() {
        return Arrays.asList(delegate);
    }

    public MessagingExceptionHandler getExceptionListener()
    {
        return this.delegate;
    }
}
