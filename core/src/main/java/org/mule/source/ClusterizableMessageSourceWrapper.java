/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
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
import org.mule.api.context.notification.ClusterNodeNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.ClusterizableMessageSource;
import org.mule.api.source.MessageSource;
import org.mule.context.notification.NotificationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Wraps a {@link ClusterizableMessageSource} in order to manage the lifecycle
 * of the wrapped instance differently depending if the node is primary or not
 * inside a cluster. Non clustered nodes are always primary.
 */
public class ClusterizableMessageSourceWrapper implements MessageSource, Lifecycle, ClusterNodeNotificationListener, MuleContextAware, FlowConstructAware
{

    protected static final Log logger = LogFactory.getLog(ClusterizableMessageSourceWrapper.class);

    private final ClusterizableMessageSource messageSource;
    private MuleContext muleContext;
    private FlowConstruct flowConstruct;
    private final Object lock = new Object();
    private boolean started;

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
        registerNotificationListener();

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
            if (!started)
            {
                if (messageSource instanceof Startable)
                {
                    if (muleContext.isPrimaryPollingInstance())
                    {
                        if (logger.isInfoEnabled())
                        {
                            logger.info("Starting clusterizable message source");
                        }
                        ((Startable) messageSource).start();

                        started = true;
                    }
                    else
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Clusterizable message source no started on secondary cluster node");
                        }
                    }
                }
            }
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

        unregisterNotificationListener();
    }

    @Override
    public void onNotification(ServerNotification notification)
    {
        if (flowConstruct != null && flowConstruct.getLifecycleState().isStarted())
        {
            try
            {
                start();
            }
            catch (MuleException e)
            {
                throw new RuntimeException("Error starting wrapped message source", e);
            }
        }
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Clusterizable message source no started on stopped flow");
            }
        }
    }

    protected void registerNotificationListener()
    {
        try
        {
            if (muleContext != null)
            {
                muleContext.registerListener(this);
            }
        }
        catch (NotificationException e)
        {
            throw new RuntimeException("Unable to register listener", e);
        }
    }

    protected void unregisterNotificationListener()
    {
        if (muleContext != null)
        {
            muleContext.unregisterListener(this);
        }
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
