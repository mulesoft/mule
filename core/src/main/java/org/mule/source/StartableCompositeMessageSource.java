/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.source;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.ClusterizableMessageSource;
import org.mule.api.source.CompositeMessageSource;
import org.mule.api.source.MessageSource;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implementation of {@link CompositeMessageSource} that propagates both injection of {@link FlowConstruct}
 * and lifecycle to nested {@link MessageSource}s.
 * <p>
 * <li>This message source cannot be started without a listener set.
 * <li>If sources are added when this composie is started they will be started as well.
 * <li>If a {@link MessageSource} is started in isolation when composite is stopped then messages will be
 * lost.
 * <li>Message will only be received from endpoints if the connector is also started.
 */
public class StartableCompositeMessageSource
    implements CompositeMessageSource, Lifecycle, FlowConstructAware, MuleContextAware
{
    protected static final Log log = LogFactory.getLog(StartableCompositeMessageSource.class);

    protected MessageProcessor listener;
    protected AtomicBoolean initialised = new AtomicBoolean(false);
    protected AtomicBoolean started = new AtomicBoolean(false);
    protected final List<MessageSource> sources = Collections.synchronizedList(new ArrayList<MessageSource>());
    protected AtomicBoolean starting = new AtomicBoolean(false);
    protected FlowConstruct flowConstruct;
    private final MessageProcessor internalListener = new InternalMessageProcessor();
    protected MuleContext muleContext;

    public void addSource(MessageSource source) throws MuleException
    {
        MessageSource messageSource = source;

        if (messageSource instanceof ClusterizableMessageSource)
        {
            messageSource = new ClusterizableMessageSourceWrapper((ClusterizableMessageSource) messageSource);
        }

        synchronized (sources)
        {
            sources.add(messageSource);
        }
        source.setListener(internalListener);
        if (initialised.get())
        {
            initializeComposedMessageSource(messageSource);
        }
        if (started.get() && source instanceof Startable)
        {
            ((Startable) source).start();
        }
    }

    private void initializeComposedMessageSource(MessageSource messageSource) throws InitialisationException
    {
        if (messageSource instanceof FlowConstructAware)
        {
            ((FlowConstructAware) messageSource).setFlowConstruct(flowConstruct);
        }
        if (messageSource instanceof MuleContextAware)
        {
            ((MuleContextAware) messageSource).setMuleContext(muleContext);
        }
        if (messageSource instanceof Initialisable)
        {
            ((Initialisable) messageSource).initialise();
        }
    }

    public void removeSource(MessageSource source) throws MuleException
    {
        if (started.get())
        {
            if (source instanceof Stoppable)
            {
                ((Stoppable) source).stop();
            }
            if (source instanceof Disposable)
            {
                ((Disposable) source).dispose();
            }
        }
        synchronized (sources)
        {
            sources.remove(source);
        }
    }
    
    public void setMessageSources(List<MessageSource> sources) throws MuleException
    {
        this.sources.clear();
        for (MessageSource messageSource : sources)
        {
            addSource(messageSource);
        }
    }
    
    public void initialise() throws InitialisationException
    {
        if (listener == null)
        {
            throw new InitialisationException(CoreMessages.objectIsNull("listener"), this);
        }
        synchronized (sources)
        {
            for (MessageSource source : sources)
            {
                initializeComposedMessageSource(source);
            }
        }
        initialised.set(true);
    }

    public void start() throws MuleException
    {
        if (listener == null)
        {
            throw new LifecycleException(CoreMessages.objectIsNull("listener"), this);
        }

        synchronized (sources)
        {
            starting.set(true);
            for (MessageSource source : sources)
            {
                if (source instanceof Startable)
                {
                    ((Startable) source).start();
                }
            }

            started.set(true);
            starting.set(false);
        }
    }

    public void stop() throws MuleException
    {
        synchronized (sources)
        {
            for (MessageSource source : sources)
            {
                if (source instanceof Stoppable)
                {
                    ((Stoppable) source).stop();
                }
            }

            started.set(false);
        }
    }
    
    public void dispose()
    {
        synchronized (sources)
        {
            for (MessageSource source : sources)
            {
                if (source instanceof Disposable)
                {
                    ((Disposable) source).dispose();
                }
            }
        }
    }

    public void setListener(MessageProcessor listener)
    {
        this.listener = listener;
    }

    public void setFlowConstruct(FlowConstruct pattern)
    {
        this.flowConstruct = pattern;

    }
    
    @Override
    public List<MessageSource> getSources()
    {
        return sources;
    }

    @Override
    public String toString()
    {
        return String.format("%s [listener=%s, sources=%s, started=%s]", getClass().getSimpleName(),
            listener, sources, started);
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        muleContext = context;
    }

    private class InternalMessageProcessor implements MessageProcessor
    {
        public InternalMessageProcessor()
        {
            super();
        }

        public MuleEvent process(MuleEvent event) throws MuleException
        {
            if (started.get() || starting.get())
            {
                return listener.process(event);
            }
            else
            {
                // TODO i18n
                throw new IllegalStateException(String.format(
                    "A message was receieved from MessageSource, but CompositeMessageSource is stopped.%n"
                                    + "  Message: %s%n" + "  CompositeMessageSource: %s", event, this));
            }
        }

        @Override
        public String toString()
        {
            return ObjectUtils.toString(this);
        }
    }
}
