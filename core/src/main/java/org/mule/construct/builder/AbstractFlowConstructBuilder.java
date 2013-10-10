/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.construct.builder;

import java.util.Collections;
import java.util.List;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.MessageSource;
import org.mule.construct.AbstractFlowConstruct;

@SuppressWarnings("unchecked")
public abstract class AbstractFlowConstructBuilder<T extends AbstractFlowConstructBuilder<?, ?>, F extends AbstractFlowConstruct>
{
    protected String name;
    protected String initialState;
    protected MessageSource messageSource;
    protected MessagingExceptionHandler exceptionListener;

    // setters should be exposed only for builders where it makes sense
    protected List<MessageProcessor> transformers = Collections.emptyList();
    protected List<MessageProcessor> responseTransformers = Collections.emptyList();

    public T name(String name)
    {
        this.name = name;
        return (T) this;
    }

    public T messageSource(MessageSource messageSource)
    {
        this.messageSource = messageSource;
        return (T) this;
    }

    public T exceptionStrategy(MessagingExceptionHandler exceptionListener)
    {
        this.exceptionListener = exceptionListener;
        return (T) this;
    }

    public T initialState(String initialState)
    {
        this.initialState = initialState;
        return (T) this;
    }

    public F build(MuleContext muleContext) throws MuleException
    {
        final F flowConstruct = buildFlowConstruct(muleContext);
        addExceptionListener(flowConstruct);
        if(initialState!=null)
        {
            flowConstruct.setInitialState(initialState);
        }
        return flowConstruct;
    }

    public F buildAndRegister(MuleContext muleContext) throws MuleException
    {
        final F flowConstruct = build(muleContext);
        muleContext.getRegistry().registerObject(flowConstruct.getName(), flowConstruct);
        return flowConstruct;
    }

    protected abstract F buildFlowConstruct(MuleContext muleContext) throws MuleException;

    protected void addExceptionListener(AbstractFlowConstruct flowConstruct)
    {
        if (exceptionListener != null)
        {
            flowConstruct.setExceptionListener(exceptionListener);
        }
        else
        {
            flowConstruct.setExceptionListener(flowConstruct.getMuleContext().getDefaultExceptionStrategy());
        }
    }
}
