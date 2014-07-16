/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.model;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.component.LifecycleAdapterFactory;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.MuleContextAware;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleState;
import org.mule.api.model.EntryPointResolver;
import org.mule.api.model.EntryPointResolverSet;
import org.mule.api.model.Model;
import org.mule.component.DefaultComponentLifecycleAdapterFactory;
import org.mule.config.i18n.CoreMessages;
import org.mule.lifecycle.EmptyLifecycleCallback;
import org.mule.model.resolvers.DefaultEntryPointResolverSet;
import org.mule.model.resolvers.LegacyEntryPointResolverSet;
import org.mule.util.ClassUtils;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>MuleModel</code> is the default implementation of the Model. The model
 * encapsulates and manages the runtime behaviour of a Mule Server instance. It is
 * responsible for maintaining the service instances and their configuration.
 */
@Deprecated
public abstract class AbstractModel implements Model
{

    public static final String DEFAULT_MODEL_NAME = "main";

    private String name = DEFAULT_MODEL_NAME;
    protected MuleContext muleContext;
    private EntryPointResolverSet entryPointResolverSet = null; // values are supplied below as required
    private LifecycleAdapterFactory lifecycleAdapterFactory = new DefaultComponentLifecycleAdapterFactory();
    private MessagingExceptionHandler exceptionListener;

    protected transient Log logger = LogFactory.getLog(getClass());

    protected ModelLifecycleManager lifecycleManager = new ModelLifecycleManager(this);

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public LifecycleState getLifecycleState()
    {
        return lifecycleManager.getState();
    }

    public EntryPointResolverSet getEntryPointResolverSet()
    {
        if (null == entryPointResolverSet)
        {
            entryPointResolverSet = new LegacyEntryPointResolverSet();
        }
        return entryPointResolverSet;
    }

    public void setEntryPointResolverSet(EntryPointResolverSet entryPointResolverSet)
    {
        this.entryPointResolverSet = entryPointResolverSet;
    }

    /**
     * This allows us to configure entry point resolvers incrementally
     *
     * @param entryPointResolvers Resolvers to add
     */
    public void setEntryPointResolvers(Collection<EntryPointResolver> entryPointResolvers)
    {
        if (null == entryPointResolverSet)
        {
            entryPointResolverSet = new DefaultEntryPointResolverSet();
        }
        
        for (EntryPointResolver resolver : entryPointResolvers)
        {
            entryPointResolverSet.addEntryPointResolver(resolver);
        }
    }

    public LifecycleAdapterFactory getLifecycleAdapterFactory()
    {
        return lifecycleAdapterFactory;
    }

    public void setLifecycleAdapterFactory(LifecycleAdapterFactory lifecycleAdapterFactory)
    {
        this.lifecycleAdapterFactory = lifecycleAdapterFactory;
    }

    /** Destroys any current components */
    public void dispose()
    {
        if(getLifecycleState().isStarted())
        {
            try
            {
                stop();
            }
            catch (MuleException e)
            {
                logger.error("Failed to stop model cleanly as part of a dispoae call: " + getName(), e);
            }
        }
        try
        {
            lifecycleManager.fireDisposePhase(new EmptyLifecycleCallback<AbstractModel>());
        }
        catch (MuleException e)
        {
            logger.error("Failed to dispose model: " + getName(), e);
        }

    }

    /**
     * Stops any registered components
     *
     * @throws MuleException if a Service fails tcomponent
     */
    public void stop() throws MuleException
    {
        lifecycleManager.fireStopPhase(new EmptyLifecycleCallback<AbstractModel>());
    }

    /**
     * Starts all registered components
     *
     * @throws MuleException if any of the components fail to start
     */
    public void start() throws MuleException
    {
        lifecycleManager.fireStartPhase(new EmptyLifecycleCallback<AbstractModel>());
    }

    public void initialise() throws InitialisationException
    {
        if (!name.equals(MuleProperties.OBJECT_SYSTEM_MODEL))
        {
            logger.warn(CoreMessages.modelDeprecated());
        }
        
        try
        {
            lifecycleManager.fireInitialisePhase(new EmptyLifecycleCallback<AbstractModel>());
        }
        catch (InitialisationException e)
        {
            throw e;
        }
        catch (MuleException e)
        {
            throw new InitialisationException(e, this);
        }
    }

    public MessagingExceptionHandler getExceptionListener()
    {
        return exceptionListener;
    }

    public void setExceptionListener(MessagingExceptionHandler exceptionListener)
    {
        this.exceptionListener = exceptionListener;
    }



    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
        //Because we allow a default Exception strategy for the model we need to inject the
        //muleContext when we get it
        if (exceptionListener == null)
        {
            exceptionListener = muleContext.getDefaultExceptionStrategy();
        }
        if (exceptionListener instanceof MuleContextAware)
        {
            ((MuleContextAware)exceptionListener).setMuleContext(muleContext);
        }
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    @Override
    public String toString()
    {
        return String.format("%s{%s}", ClassUtils.getSimpleName(this.getClass()), getName());
    }
}
