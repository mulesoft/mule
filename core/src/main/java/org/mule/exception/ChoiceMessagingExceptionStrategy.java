/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.exception;

import org.mule.api.GlobalNameableObject;
import org.mule.api.MuleEvent;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.exception.MessagingExceptionHandlerAcceptor;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.processor.MessageProcessorContainer;
import org.mule.api.processor.MessageProcessorPathElement;
import org.mule.config.i18n.CoreMessages;
import org.mule.message.DefaultExceptionPayload;
import org.mule.processor.AbstractMuleObjectOwner;

import java.util.Collections;
import java.util.List;

/**
 * Selects which exception strategy to execute based on filtering.
 * <p/>
 * Exception listeners must implement {@link org.mule.api.exception.MessagingExceptionHandlerAcceptor} to be part of ChoiceMessagingExceptionStrategy
 */
public class ChoiceMessagingExceptionStrategy extends AbstractMuleObjectOwner<MessagingExceptionHandlerAcceptor>
        implements MessagingExceptionHandlerAcceptor, MuleContextAware, Lifecycle, MessageProcessorContainer, GlobalNameableObject
{

    private List<MessagingExceptionHandlerAcceptor> exceptionListeners;

    protected String globalName;

    public String getGlobalName()
    {
        return globalName;
    }

    public void setGlobalName(String globalName)
    {
        this.globalName = globalName;
    }

    @Override
    public MuleEvent handleException(Exception exception, MuleEvent event)
    {
        event.getMessage().setExceptionPayload(new DefaultExceptionPayload(exception));
        for (MessagingExceptionHandlerAcceptor exceptionListener : exceptionListeners)
        {
            if (exceptionListener.accept(event))
            {
                event.getMessage().setExceptionPayload(null);
                return exceptionListener.handleException(exception, event);
            }
        }
        throw new MuleRuntimeException(CoreMessages.createStaticMessage("Default exception strategy must accept any event."));
    }

    public void setExceptionListeners(List<MessagingExceptionHandlerAcceptor> exceptionListeners)
    {
        this.exceptionListeners = exceptionListeners;
    }

    public List<MessagingExceptionHandlerAcceptor> getExceptionListeners()
    {
        return Collections.unmodifiableList(exceptionListeners);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        addDefaultExceptionStrategyIfRequired();
        super.initialise();
        validateConfiguredExceptionStrategies();
    }

    private void addDefaultExceptionStrategyIfRequired() throws InitialisationException
    {
        if (!exceptionListeners.get(exceptionListeners.size() - 1).acceptsAll())
        {
            MessagingExceptionHandler defaultExceptionStrategy;
            try
            {
                defaultExceptionStrategy = getMuleContext().getDefaultExceptionStrategy();
            }
            catch (Exception e)
            {
                throw new InitialisationException(CoreMessages.createStaticMessage("Failure initializing " +
                                                                                   "choice-exception-strategy. If choice-exception-strategy is defined as default one " +
                                                                                   "check that last exception strategy inside choice catchs all"), e, this);
            }
            this.exceptionListeners.add(new MessagingExceptionStrategyAcceptorDelegate(defaultExceptionStrategy));
        }
    }

    @Override
    protected List<MessagingExceptionHandlerAcceptor> getOwnedObjects()
    {
        return Collections.unmodifiableList(exceptionListeners);
    }

    private void validateConfiguredExceptionStrategies()
    {
        validateOnlyLastAcceptsAll();
        validateOnlyOneHandlesRedelivery();
    }

    private void validateOnlyOneHandlesRedelivery()
    {
        boolean rollbackWithRedelivery = false;
        for (int i = 0; i < exceptionListeners.size(); i++)
        {
            MessagingExceptionHandler messagingExceptionHandler = exceptionListeners.get(i);
            if (messagingExceptionHandler instanceof MessagingExceptionStrategyAcceptorDelegate)
            {
                messagingExceptionHandler = ((MessagingExceptionStrategyAcceptorDelegate) messagingExceptionHandler).getExceptionListener();
            }
            if (messagingExceptionHandler instanceof RollbackMessagingExceptionStrategy && ((RollbackMessagingExceptionStrategy) messagingExceptionHandler).hasMaxRedeliveryAttempts())
            {
                if (rollbackWithRedelivery)
                {
                    throw new MuleRuntimeException(CoreMessages.createStaticMessage("Only one rollback exception strategy inside <choice-exception-strategy> can handle message redelivery."));
                }
                rollbackWithRedelivery = true;
            }
        }
    }

    private void validateOnlyLastAcceptsAll()
    {
        for (int i = 0; i < exceptionListeners.size() - 1; i++)
        {
            MessagingExceptionHandlerAcceptor messagingExceptionHandlerAcceptor = exceptionListeners.get(i);
            if (messagingExceptionHandlerAcceptor.acceptsAll())
            {
                throw new MuleRuntimeException(CoreMessages.createStaticMessage("Only last exception strategy inside <choice-exception-strategy> can accept any message. Maybe expression attribute is empty."));
            }
        }
    }

    @Override
    public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement)
    {
        int idx = 0;
        for (MessagingExceptionHandlerAcceptor listener : exceptionListeners)
        {
            if (listener instanceof MessageProcessorContainer)
            {
                MessageProcessorPathElement exceptionListener = pathElement.addChild(String.valueOf(idx));
                ((MessageProcessorContainer) listener).addMessageProcessorPathElements(exceptionListener);
            }
            idx++;
        }

    }

    @Override
    public boolean accept(MuleEvent event)
    {
        return true;
    }

    @Override
    public boolean acceptsAll()
    {
        return true;
    }
}
