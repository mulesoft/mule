/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.source;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.Pattern;
import org.mule.api.PatternAware;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.CompositeMessageSource;
import org.mule.api.source.MessageSource;
import org.mule.config.i18n.CoreMessages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implementation of {@link CompositeMessageSource} that propagates both injection of
 * {@link Pattern} and lifecycle to nested {@link MessageSource}s.
 * <p>
 * <li>This MessageSourceAggregator cannot be started without a listener set.
 * <li>If sources are added when this composie is started they will be started.
 * <li>If a MessageSource is started in isolation when composite is stopped then
 * messages will be lost.
 * <li>Message will only be received from endpoints if the connector is also started.
 */
public class StartablePatternAwareCompositeMessageSource
    implements CompositeMessageSource, Startable, Stoppable, PatternAware
{
    private static final Log log = LogFactory.getLog(StartablePatternAwareCompositeMessageSource.class);

    private MessageProcessor listener;
    private MessageProcessor internalListener = new InternalMessageProcessor();
    private List<MessageSource> sources = Collections.synchronizedList(new ArrayList<MessageSource>());
    private AtomicBoolean started = new AtomicBoolean(false);
    private Pattern pattern;

    public void addSource(MessageSource source) throws MuleException
    {
        sources.add(source);
        source.setListener(internalListener);
        if (started.get())
        {
            if (source instanceof PatternAware)
            {
                ((PatternAware) source).setPattern(pattern);
            }
            if (source instanceof Startable)
            {
                ((Startable) source).start();
            }
        }
    }

    public void removeSource(MessageSource source) throws MuleException
    {
        if (started.get() && source instanceof Stoppable)
        {
            ((Stoppable) source).stop();
        }
        sources.remove(source);
    }

    public void setListener(MessageProcessor listener)
    {
        this.listener = listener;
    }

    public void start() throws MuleException
    {
        if (listener == null)
        {
            throw new LifecycleException(CoreMessages.objectIsNull("listener"), this);
        }
        synchronized (sources)
        {
            for (MessageSource source : sources)
            {
                if (source instanceof PatternAware)
                {
                    ((PatternAware) source).setPattern(pattern);
                }
                if (source instanceof Startable)
                {
                    ((Startable) source).start();
                }
            }
            started.set(true);
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

    public void setPattern(Pattern pattern)
    {
        this.pattern = pattern;

    }

    @Override
    public String toString()
    {
        return "StartableMessageSourceAgregator [listener=" + listener + ", sources=" + sources
               + ", started=" + started + "]";
    }

    private class InternalMessageProcessor implements MessageProcessor
    {
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            if (started.get())
            {
                return listener.process(event);
            }
            else
            {
                log.warn("Message " + event
                         + " was recieved from MessageSource, but MessageSourceAggregator " + this
                         + " is stopped.  Message will be discarded.");
                return null;
            }

        }
    }
}
