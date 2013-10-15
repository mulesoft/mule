/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import org.mule.api.AnnotatedObject;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;

/**
 * An object that owns message processors and delegates startup/shutdown events to them.
 */
public abstract class AbstractMessageProcessorOwner implements Lifecycle, MuleContextAware, FlowConstructAware, AnnotatedObject
{
    protected MuleContext muleContext;
    protected FlowConstruct flowConstruct;
    private final Map<QName, Object> annotations = new ConcurrentHashMap<QName, Object>();

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

    public void initialise() throws InitialisationException
    {
        for (MessageProcessor processor : getOwnedMessageProcessors())
        {
            if (processor instanceof MuleContextAware)
            {
                ((MuleContextAware) processor).setMuleContext(muleContext);
            }
            if (processor instanceof FlowConstructAware)
            {
                ((FlowConstructAware) processor).setFlowConstruct(flowConstruct);
            }
            if (processor instanceof Initialisable)
            {
                ((Initialisable) processor).initialise();
            }
        }
    }

    public void dispose()
    {
        for (MessageProcessor processor : getOwnedMessageProcessors())
        {

            if (processor instanceof Disposable)
            {
                ((Disposable) processor).dispose();
            }
        }
    }


    public void start() throws MuleException
    {

        for (MessageProcessor processor : getOwnedMessageProcessors())
        {
            if (processor instanceof Startable)
            {
                ((Startable) processor).start();
            }
        }
    }


    public void stop() throws MuleException
    {

        for (MessageProcessor processor : getOwnedMessageProcessors())
        {
            if (processor instanceof Stoppable)
            {
                ((Stoppable) processor).stop();
            }

        }
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

    protected abstract List<MessageProcessor> getOwnedMessageProcessors();

}

