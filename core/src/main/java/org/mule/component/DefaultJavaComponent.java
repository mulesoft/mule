/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.component;

import org.mule.api.MuleException;
import org.mule.api.component.InterfaceBinding;
import org.mule.api.component.JavaComponent;
import org.mule.api.component.LifecycleAdapter;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.model.EntryPointResolverSet;
import org.mule.api.object.ObjectFactory;
import org.mule.api.registry.ServiceException;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;

import java.util.List;

/**
 * Default implementation of {@link JavaComponent}. Component lifecycle is
 * propagated to the component object instance via the {@link LifecycleAdapter}.
 */
public class DefaultJavaComponent extends AbstractJavaComponent
{

    protected LifecycleAdapter singletonComponentLifecycleAdapter;

    /**
     * For spring only
     */
    public DefaultJavaComponent()
    {
        super();
    }

    public DefaultJavaComponent(ObjectFactory objectFactory)
    {
        super(objectFactory);
    }

    public DefaultJavaComponent(ObjectFactory objectFactory,
                                EntryPointResolverSet entryPointResolverSet,
                                List<InterfaceBinding> bindings)
    {
        super(objectFactory, entryPointResolverSet, bindings);
    }

    @Override
    protected void doStart() throws MuleException
    {
        super.doStart();

        // If this component is using a SingletonObjectFactory we should create
        // LifecycleAdaptor wrapper just once now and not on each event. This also
        // allows start/stop life-cycle methods to be propagated to singleton
        // component instances.
        if (objectFactory != null && objectFactory.isSingleton())
        {
            // On first call, create and initialise singleton instance
            try
            {
                if (singletonComponentLifecycleAdapter == null)
                {
                    singletonComponentLifecycleAdapter = createLifecycleAdaptor();
                }
            }
            catch (Exception e)
            {
                throw new InitialisationException(
                    MessageFactory.createStaticMessage("Unable to create instance of POJO service"), e, this);

            }
            // On all calls, start if not started.
            if (!singletonComponentLifecycleAdapter.isStarted())
            {
                try
                {
                    singletonComponentLifecycleAdapter.start();
                }
                catch (Exception e)
                {
                    throw new ServiceException(CoreMessages.failedToStart("Service '" + flowConstruct.getName() + "'"), e);
                }
            }
        }
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        for (InterfaceBinding binding : bindings)
        {
            applyLifecycleAndDependencyInjection(binding);
        }
    }

    @Override
    protected void doStop() throws MuleException
    {
        super.doStop();
        // It only makes sense to propagate this life-cycle to singleton component
        // implementations
        if (singletonComponentLifecycleAdapter != null && singletonComponentLifecycleAdapter.isStarted())
        {
            try
            {
                singletonComponentLifecycleAdapter.stop();
            }
            catch (Exception e)
            {
                throw new ServiceException(CoreMessages.failedToStop("Service '" + flowConstruct.getName() + "'"), e);
            }
        }
    }

    @Override
    protected void doDispose()
    {
        super.doDispose();
        // It only makes sense to propagating this life-cycle to singleton component
        // implementations
        if (singletonComponentLifecycleAdapter != null)
        {
            singletonComponentLifecycleAdapter.dispose();
        }
    }

    @Override
    protected LifecycleAdapter borrowComponentLifecycleAdaptor() throws Exception
    {
        LifecycleAdapter componentLifecycleAdapter;
        if (singletonComponentLifecycleAdapter != null)
        {
            componentLifecycleAdapter = singletonComponentLifecycleAdapter;
        }
        else
        {
            componentLifecycleAdapter = createLifecycleAdaptor();
            componentLifecycleAdapter.start();
        }
        return componentLifecycleAdapter;
    }

    @Override
    protected void returnComponentLifecycleAdaptor(LifecycleAdapter lifecycleAdapter) throws Exception
    {
        if (singletonComponentLifecycleAdapter == null && lifecycleAdapter != null)
        {
            lifecycleAdapter.stop();
            lifecycleAdapter.dispose();
        }
    }

}
