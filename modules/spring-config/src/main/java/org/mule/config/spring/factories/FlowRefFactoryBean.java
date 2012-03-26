/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.factories;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.CoreMessages;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class FlowRefFactoryBean
    implements FactoryBean<MessageProcessor>, ApplicationContextAware, MuleContextAware, Initialisable
{
    private String refName;
    private ApplicationContext applicationContext;
    private MuleContext muleContext;
    private MessageProcessor referencedMessageProcessor;

    public void setName(String name)
    {
        this.refName = name;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (refName.isEmpty())
        {
            throw new InitialisationException(CoreMessages.objectIsNull("flow reference is empty"), this);
        }
        else if (!muleContext.getExpressionManager().isExpression(refName))
        {
            referencedMessageProcessor = getReferencedMessageProcessor(refName);
        }
    }

    @Override
    public MessageProcessor getObject() throws Exception
    {
        if (referencedMessageProcessor != null)
        {
            return referencedMessageProcessor;
        }
        else
        {
            return new MessageProcessor()
            {
                @Override
                public MuleEvent process(MuleEvent event) throws MuleException
                {
                    MessageProcessor dynamicMessageProcessor = getReferencedMessageProcessor(muleContext.getExpressionManager()
                        .parse(refName, event));
                    return dynamicMessageProcessor.process(event);
                }
            };
        }
    }

    protected MessageProcessor getReferencedMessageProcessor(String name)
    {
        final MessageProcessor processor = ((MessageProcessor) applicationContext.getBean(name));
        if (processor == null)
        {
            throw new MuleRuntimeException(CoreMessages.objectIsNull(name));
        }
        else if (processor instanceof FlowConstruct)
        {
            // If a FlowConstuct is reference then decouple lifcycle/injection
            return new MessageProcessor()
            {
                @Override
                public MuleEvent process(MuleEvent event) throws MuleException
                {
                    return processor.process(event);
                }
            };
        }
        else
        {
            return processor;
        }
    }

    @Override
    public boolean isSingleton()
    {
        return false;
    }

    @Override
    public Class<?> getObjectType()
    {
        return MessageProcessor.class;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }
}
