/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.model;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.component.LifecycleAdapterFactory;
import org.mule.api.context.MuleContextAware;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.model.EntryPointResolver;
import org.mule.api.model.EntryPointResolverSet;
import org.mule.api.model.Model;
import org.mule.component.DefaultLifecycleAdapterFactory;
import org.mule.context.notification.ModelNotification;
import org.mule.model.resolvers.DefaultEntryPointResolverSet;
import org.mule.model.resolvers.LegacyEntryPointResolverSet;
import org.mule.service.DefaultServiceExceptionStrategy;

import java.beans.ExceptionListener;
import java.util.Collection;
import java.util.Iterator;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>MuleModel</code> is the default implementation of the Model. The model
 * encapsulates and manages the runtime behaviour of a Mule Server instance. It is
 * responsible for maintaining the service instances and their configuration.
 */
public abstract class AbstractModel implements Model
{

    public static final String DEFAULT_MODEL_NAME = "main";

    private String name = DEFAULT_MODEL_NAME;
    private EntryPointResolverSet entryPointResolverSet = null; // values are supplied below as required
    private LifecycleAdapterFactory lifecycleAdapterFactory = new DefaultLifecycleAdapterFactory();
    private AtomicBoolean initialised = new AtomicBoolean(false);
    private AtomicBoolean started = new AtomicBoolean(false);
    private ExceptionListener exceptionListener = new DefaultServiceExceptionStrategy();

    protected transient Log logger = LogFactory.getLog(getClass());
    protected MuleContext muleContext;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.api.model.Model#getEntryPointResolver()
     */
    public EntryPointResolverSet getEntryPointResolverSet()
    {
        if (null == entryPointResolverSet)
        {
            entryPointResolverSet = new LegacyEntryPointResolverSet();
        }
        return entryPointResolverSet;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.api.model.Model#setEntryPointResolver(org.mule.api.model.EntryPointResolver)
     */
    public void setEntryPointResolverSet(EntryPointResolverSet entryPointResolverSet)
    {
        this.entryPointResolverSet = entryPointResolverSet;
    }

    /**
     * This allows us to configure entry point resolvers incrementally
     *
     * @param entryPointResolvers Resolvers to add
     */
    public void setEntryPointResolvers(Collection entryPointResolvers)
    {
        if (null == entryPointResolverSet)
        {
            entryPointResolverSet = new DefaultEntryPointResolverSet();
        }
        for (Iterator resolvers = entryPointResolvers.iterator(); resolvers.hasNext();)
        {
            entryPointResolverSet.addEntryPointResolver((EntryPointResolver) resolvers.next());
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.api.model.Model#getLifecycleAdapterFactory()
     */
    public LifecycleAdapterFactory getLifecycleAdapterFactory()
    {
        return lifecycleAdapterFactory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.api.model.Model#setLifecycleAdapterFactory(org.mule.api.lifecycle.LifecycleAdapterFactory)
     */
    public void setLifecycleAdapterFactory(LifecycleAdapterFactory lifecycleAdapterFactory)
    {
        this.lifecycleAdapterFactory = lifecycleAdapterFactory;
    }

    /** Destroys any current components */
    public void dispose()
    {
        fireNotification(new ModelNotification(this, ModelNotification.MODEL_DISPOSING));
        fireNotification(new ModelNotification(this, ModelNotification.MODEL_DISPOSED));
    }

    /**
     * Stops any registered components
     *
     * @throws MuleException if a Service fails tcomponent
     */
    public void stop() throws MuleException
    {
        fireNotification(new ModelNotification(this, ModelNotification.MODEL_STOPPING));
        started.set(false);
        fireNotification(new ModelNotification(this, ModelNotification.MODEL_STOPPED));
    }

    /**
     * Starts all registered components
     *
     * @throws MuleException if any of the components fail to start
     */
    public void start() throws MuleException
    {
        if (!initialised.get())
        {
            throw new IllegalStateException("Not Initialised");
        }

        if (!started.get())
        {
            fireNotification(new ModelNotification(this, ModelNotification.MODEL_STARTING));
            started.set(true);
            fireNotification(new ModelNotification(this, ModelNotification.MODEL_STARTED));
        }
        else
        {
            logger.debug("Model already started");
        }
    }

    public void initialise() throws InitialisationException
    {
        if (!initialised.get())
        {
            fireNotification(new ModelNotification(this, ModelNotification.MODEL_INITIALISING));
            initialised.set(true);
            fireNotification(new ModelNotification(this, ModelNotification.MODEL_INITIALISED));
        }
        else
        {
            logger.debug("Model already initialised");
        }
    }

    public ExceptionListener getExceptionListener()
    {
        return exceptionListener;
    }

    public void setExceptionListener(ExceptionListener exceptionListener)
    {
        this.exceptionListener = exceptionListener;
    }

    void fireNotification(ServerNotification notification)
    {
        if (muleContext != null)
        {
            muleContext.fireNotification(notification);
        }
        else if (logger.isWarnEnabled())
        {
            logger.debug("MuleContext is not yet available for firing notifications, ignoring event: " + notification);
        }
    }

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
        //Because we allow a default Exception strategy for the model we need to inject the
        //muleContext when we get it
        if(exceptionListener instanceof MuleContextAware)
        {
            ((MuleContextAware)exceptionListener).setMuleContext(muleContext);
        }
    }

}
