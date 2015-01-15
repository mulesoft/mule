/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.factories;

import org.mule.api.AnnotatedObject;
import org.mule.api.MuleContext;
import org.mule.api.NameableObject;
import org.mule.api.context.MuleContextAware;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorBuilder;
import org.mule.api.processor.ProcessingStrategy;
import org.mule.processor.AsyncDelegateMessageProcessor;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.FactoryBean;

public class AsyncMessageProcessorsFactoryBean implements FactoryBean, MuleContextAware, NameableObject, AnnotatedObject
{

    protected MuleContext muleContext;

    protected List messageProcessors;
    protected ProcessingStrategy processingStrategy;
    protected String name;
    private final Map<QName, Object> annotations = new ConcurrentHashMap<QName, Object>();

    public Class getObjectType()
    {
        return MessageProcessor.class;
    }

    public void setMessageProcessors(List messageProcessors)
    {
        this.messageProcessors = messageProcessors;
    }

    public Object getObject() throws Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.setName("'async' child chain");

        for (Object processor : messageProcessors)
        {
            if (processor instanceof MessageProcessor)
            {
                builder.chain((MessageProcessor) processor);
            }
            else if (processor instanceof MessageProcessorBuilder)
            {
                builder.chain((MessageProcessorBuilder) processor);
            }
            else
            {
                throw new IllegalArgumentException(
                    "MessageProcessorBuilder should only have MessageProcessor's or MessageProcessorBuilder's configured");
            }
        }
        AsyncDelegateMessageProcessor delegate = new AsyncDelegateMessageProcessor(builder.build(),
            processingStrategy, name);
        delegate.setAnnotations(getAnnotations());
        return delegate;
    }

    public boolean isSingleton()
    {
        return false;
    }

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setProcessingStrategy(ProcessingStrategy processingStrategy)
    {
        this.processingStrategy = processingStrategy;
    }

    public final Object getAnnotation(QName name)
    {
        return annotations.get(name);
    }

    public final Map<QName, Object> getAnnotations()
    {
        return Collections.unmodifiableMap(annotations);
    }

    public synchronized final void setAnnotations(Map<QName, Object> newAnnotations)
    {
        annotations.clear();
        annotations.putAll(newAnnotations);
    }
}
