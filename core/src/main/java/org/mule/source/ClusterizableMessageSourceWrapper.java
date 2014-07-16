/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.source;

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
import org.mule.api.source.ClusterizableMessageSource;
import org.mule.api.source.MessageSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.lifecycle.PrimaryNodeLifecycleNotificationListener;

/**
 * Wraps a {@link ClusterizableMessageSource} in order to manage the lifecycle
 * of the wrapped instance differently depending if the node is primary or not
 * inside a cluster. Non clustered nodes are always primary.
 */
public class ClusterizableMessageSourceWrapper implements MessageSource, Lifecycle,  MuleContextAware, FlowConstructAware
{

    protected static final Log logger = LogFactory.getLog(ClusterizableMessageSourceWrapper.class);

    private PrimaryNodeLifecycleNotificationListener primaryNodeLifecycleNotificationListener;
    private final ClusterizableMessageSource messageSource;
    private MuleContext muleContext;
    private FlowConstruct flowConstruct;
    private final Object lock = new Object();
    private boolean started;
    private boolean messageSourceStarted;

    public ClusterizableMessageSourceWrapper(ClusterizableMessageSource messageSource)
    {
        this.messageSource = messageSource;
    }

    public ClusterizableMessageSourceWrapper(MuleContext muleContext, ClusterizableMessageSource messageSource, FlowConstruct flowConstruct)
    {
        this.messageSource = messageSource;
        setMuleContext(muleContext);
        setFlowConstruct(flowConstruct);
    }

    @Override
    public void setListener(MessageProcessor listener)
    {
        messageSource.setListener(listener);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        primaryNodeLifecycleNotificationListener = new PrimaryNodeLifecycleNotificationListener(new Startable() {
            @Override
            public void start() throws MuleException {
                if (ClusterizableMessageSourceWrapper.this.isStarted())
                {
                    ClusterizableMessageSourceWrapper.this.start();
                }
            }
        },muleContext);

        primaryNodeLifecycleNotificationListener.register();

        if (messageSource instanceof Initialisable)
        {
            ((Initialisable) messageSource).initialise();
        }
    }

    @Override
    public void start() throws MuleException
    {
        synchronized (lock)
        {
            if (messageSourceStarted)
            {
                return;
            }
            if (messageSource instanceof Startable)
            {
                if (muleContext.isPrimaryPollingInstance())
                {
                    if (logger.isInfoEnabled())
                    {
                        logger.info("Starting clusterizable message source");
                    }
                    ((Startable) messageSource).start();
                    messageSourceStarted = true;
                }
                else
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Clusterizable message source no started on secondary cluster node");
                    }
                }
            }
            started = true;
        }
    }

    @Override
    public void stop() throws MuleException
    {
        synchronized (lock)
        {
            if (started)
            {
                if (messageSource instanceof Stoppable)
                {
                    ((Stoppable) messageSource).stop();
                }
                started = false;
                messageSourceStarted = false;
            }
        }
    }

    @Override
    public void dispose()
    {
        if (messageSource instanceof Disposable)
        {
            ((Disposable) messageSource).dispose();
        }

        primaryNodeLifecycleNotificationListener.unregister();
    }


    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
        if (messageSource instanceof FlowConstructAware)
        {
            ((FlowConstructAware) messageSource).setFlowConstruct(flowConstruct);
        }
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        muleContext = context;

        if (messageSource instanceof MuleContextAware)
        {
            ((MuleContextAware) messageSource).setMuleContext(muleContext);
        }
    }

    public boolean isStarted()
    {
        return started;
    }
}
