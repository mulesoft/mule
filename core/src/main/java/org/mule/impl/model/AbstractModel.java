/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.model;

import org.mule.api.MuleContext;
import org.mule.impl.DefaultComponentExceptionStrategy;
import org.mule.impl.DefaultLifecycleAdapterFactory;
import org.mule.impl.internal.notifications.ModelNotification;
import org.mule.impl.model.resolvers.DefaultEntryPointResolverSet;
import org.mule.impl.model.resolvers.LegacyEntryPointResolverSet;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.UMOLifecycleAdapterFactory;
import org.mule.umo.manager.UMOServerNotification;
import org.mule.umo.model.UMOEntryPointResolver;
import org.mule.umo.model.UMOEntryPointResolverSet;
import org.mule.umo.model.UMOModel;

import java.beans.ExceptionListener;
import java.util.Collection;
import java.util.Iterator;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>MuleModel</code> is the default implementation of the UMOModel. The model
 * encapsulates and manages the runtime behaviour of a Mule Server instance. It is
 * responsible for maintaining the UMOs instances and their configuration.
 */
public abstract class AbstractModel implements UMOModel
{

    public static final String DEFAULT_MODEL_NAME = "main";

    private String name = DEFAULT_MODEL_NAME;
    private UMOEntryPointResolverSet entryPointResolverSet = null; // values are supplied below as required
    private UMOLifecycleAdapterFactory lifecycleAdapterFactory = new DefaultLifecycleAdapterFactory();
    private AtomicBoolean initialised = new AtomicBoolean(false);
    private AtomicBoolean started = new AtomicBoolean(false);
    private ExceptionListener exceptionListener = new DefaultComponentExceptionStrategy();

    protected transient Log logger = LogFactory.getLog(getClass());
    protected MuleContext muleContext;

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOModel#getName()
     */
    public String getName()
    {
        return name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOModel#setName(java.lang.String)
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.model.UMOModel#getEntryPointResolver()
     */
    public UMOEntryPointResolverSet getEntryPointResolverSet()
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
     * @see org.mule.umo.model.UMOModel#setEntryPointResolver(org.mule.umo.model.UMOEntryPointResolver)
     */
    public void setEntryPointResolverSet(UMOEntryPointResolverSet entryPointResolverSet)
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
            entryPointResolverSet.addEntryPointResolver((UMOEntryPointResolver) resolvers.next());
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.model.UMOModel#getLifecycleAdapterFactory()
     */
    public UMOLifecycleAdapterFactory getLifecycleAdapterFactory()
    {
        return lifecycleAdapterFactory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.model.UMOModel#setLifecycleAdapterFactory(org.mule.umo.lifecycle.UMOLifecycleAdapterFactory)
     */
    public void setLifecycleAdapterFactory(UMOLifecycleAdapterFactory lifecycleAdapterFactory)
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
     * @throws UMOException if a Component fails tcomponent
     */
    public void stop() throws UMOException
    {
        fireNotification(new ModelNotification(this, ModelNotification.MODEL_STOPPING));
        started.set(false);
        fireNotification(new ModelNotification(this, ModelNotification.MODEL_STOPPED));
    }

    /**
     * Starts all registered components
     *
     * @throws UMOException if any of the components fail to start
     */
    public void start() throws UMOException
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

    void fireNotification(UMOServerNotification notification)
    {
        if (muleContext != null)
        {
            muleContext.fireNotification(notification);
        }
        else if (logger.isDebugEnabled())
        {
            logger.debug("MuleContext is not yet available for firing notifications, ignoring event: " + notification);
        }
    }

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

}
