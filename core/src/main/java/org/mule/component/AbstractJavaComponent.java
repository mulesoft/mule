/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.component;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.component.JavaComponent;
import org.mule.api.component.LifecycleAdapter;
import org.mule.api.component.LifecycleAdapterFactory;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.model.EntryPointResolver;
import org.mule.api.model.EntryPointResolverSet;
import org.mule.api.object.ObjectFactory;
import org.mule.api.routing.BindingCollection;
import org.mule.config.i18n.CoreMessages;
import org.mule.model.resolvers.DefaultEntryPointResolverSet;
import org.mule.routing.binding.DefaultBindingCollection;

import java.util.Collection;
import java.util.Iterator;

/**
 * Abstract implementation of JavaComponent adds JavaComponent specific's:
 * {@link EntryPointResolverSet}, {@link org.mule.api.routing.BindingCollection} and
 * {@link ObjectFactory}. Provides default implementations of doOnCall and doOnEvent
 * and defines abstract template methods provided for obtaining and returning the
 * component object instance.
 */
public abstract class AbstractJavaComponent extends AbstractComponent implements JavaComponent
{

    protected EntryPointResolverSet entryPointResolverSet;

    protected BindingCollection bindingCollection = new DefaultBindingCollection();

    protected ObjectFactory objectFactory;

    protected LifecycleAdapterFactory lifecycleAdapterFactory;

    public AbstractJavaComponent()
    {
        // For Spring only
        super();
    }

    public AbstractJavaComponent(ObjectFactory objectFactory)
    {
        this(objectFactory, null, null);
    }

    public AbstractJavaComponent(ObjectFactory objectFactory,
                                 EntryPointResolverSet entryPointResolverSet,
                                 BindingCollection bindingCollection)
    {
        super();
        this.objectFactory = objectFactory;
        this.entryPointResolverSet = entryPointResolverSet;
        if (bindingCollection != null)
        {
            this.bindingCollection = bindingCollection;
        }
    }

    protected Object doInvoke(MuleEvent event) throws Exception
    {
        return invokeComponentInstance(event);
    }

    protected Object invokeComponentInstance(MuleEvent event) throws Exception
    {
        LifecycleAdapter componentLifecycleAdapter = null;
        try
        {
            componentLifecycleAdapter = borrowComponentLifecycleAdaptor();
            return componentLifecycleAdapter.invoke(event);
        }
        finally
        {
            returnComponentLifecycleAdaptor(componentLifecycleAdapter);
        }
    }

    public Class getObjectType()
    {
        return objectFactory.getObjectClass();
    }

    /**
     * Creates and initialises a new LifecycleAdaptor instance wrapped the component
     * object instance obtained from the configured object factory.
     * 
     * @return
     * @throws MuleException
     * @throws Exception
     */
    protected LifecycleAdapter createLifeCycleAdaptor() throws MuleException, Exception
    {
        LifecycleAdapter lifecycleAdapter;
        if (lifecycleAdapterFactory != null)
        {
            // Custom lifecycleAdapterFactory set on component
            lifecycleAdapter = lifecycleAdapterFactory.create(objectFactory.getInstance(), this, entryPointResolverSet, muleContext);
        }
        else if (objectFactory.isExternallyManagedLifecycle())
        {
            // If no lifecycleAdapterFactory is configured explicitly and object factory returns externally managed instance then 
            // use NullLifecycleAdapter so that lifecycle is not propagated
            lifecycleAdapter = new NullLifecycleAdapter(objectFactory.getInstance(), this,
                entryPointResolverSet, muleContext);
        }
        else
        {
            // Inherit lifecycleAdapterFactory from model
            lifecycleAdapter = service.getModel().getLifecycleAdapterFactory().create(objectFactory.getInstance(),
                this, entryPointResolverSet, muleContext);
        }
        lifecycleAdapter.initialise();
        return lifecycleAdapter;
    }

    protected abstract LifecycleAdapter borrowComponentLifecycleAdaptor() throws Exception;

    protected abstract void returnComponentLifecycleAdaptor(LifecycleAdapter lifecycleAdapter) throws Exception;

    @Override
    protected void doInitialise() throws InitialisationException
    {
        if (objectFactory == null)
        {
            throw new InitialisationException(CoreMessages.objectIsNull("object factory"), this);
        }
        // If this component was configured with spring the objectFactory instance
        // has already been initialised, yet if this component was no configured with
        // spring then the objectFactory is still uninitialised so we need to
        // initialise it here.
        objectFactory.initialise();
    }

    @Override
    protected void doStart() throws MuleException
    {
        // We need to resolve entry point resolvers here rather than in initialise()
        // because when configuring with spring, although the service has been
        // injected and is available the injected service construction has not been
        // completed and model is still in null.
        if (entryPointResolverSet == null)
        {
            entryPointResolverSet = service.getModel().getEntryPointResolverSet();
        }
    }

    @Override
    protected void doDispose()
    {
        // TODO This can't be implemented currently because AbstractService allows
        // disposed services to be re-initialised, and re-use of a disposed object
        // factory is not possible
        // objectFactory.dispose();
    }

    public EntryPointResolverSet getEntryPointResolverSet()
    {
        return entryPointResolverSet;
    }

    public BindingCollection getBindingCollection()
    {
        return bindingCollection;
    }

    public void setEntryPointResolverSet(EntryPointResolverSet entryPointResolverSet)
    {
        this.entryPointResolverSet = entryPointResolverSet;
    }

    public void setBindingCollection(BindingCollection bindingCollection)
    {
        this.bindingCollection = bindingCollection;
    }

    /**
     * Allow for incremental addition of resolvers by for example the spring-config
     * module
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

    public ObjectFactory getObjectFactory()
    {
        return objectFactory;
    }

    public void setObjectFactory(ObjectFactory objectFactory)
    {
        this.objectFactory = objectFactory;
    }

    public LifecycleAdapterFactory getLifecycleAdapterFactory()
    {
        return lifecycleAdapterFactory;
    }

    public void setLifecycleAdapterFactory(LifecycleAdapterFactory lifecycleAdapterFactory)
    {
        this.lifecycleAdapterFactory = lifecycleAdapterFactory;
    }

}
