/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.guice;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.NamedObject;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.registry.RegistrationException;
import org.mule.endpoint.DefaultEndpointFactory;
import org.mule.module.guice.i18n.GuiceMessages;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

/**
 * A mule specific Guice module that allows users to override the {@link #configureMuleContext(org.mule.api.MuleContext)} method
 * to do any Mule configuration such as register notifications.  Most users will not need to override this method so the
 * {@link com.google.inject.AbstractModule} can be used.
 * <p/>
 * Note that Mule objects such as Connectors and Agents can be registered in a Guice module too.  To do this create provider methods
 * on a module and mark with the {@link com.google.inject.Provides} annotation.
 * <p/>
 * Its recommended that you put all your Mule configuration objects in a separate Guice module.
 */
public abstract class AbstractMuleGuiceModule extends AbstractModule
{

    protected MuleContext muleContext;


    void setMuleContext(MuleContext context)
    {
        muleContext = context;
        configureMuleContext(muleContext);
    }

    public void configureMuleContext(MuleContext muleContext)
    {

    }


    protected final void configure()
    {
        bindListener(Matchers.any(), new TypeListener()
        {
            public <I> void hear(TypeLiteral<I> iTypeLiteral, TypeEncounter<I> iTypeEncounter)
            {
                iTypeEncounter.register(new MuleRegistryInjectionLister());
            }
        });
        bind(MuleContext.class).toInstance(muleContext);
        try
        {
            doConfigure();
        }
        catch (Exception e)
        {
            addError(e);
        }
    }

    protected abstract void doConfigure() throws Exception;

    /**
     * Creates an {@link org.mule.api.endpoint.EndpointBuilder} instance for the endpoint uri.  The builder can be used to add
     * further configuration options and then used to create either {@link org.mule.api.endpoint.OutboundEndpoint} or
     * {@link org.mule.api.endpoint.InboundEndpoint} instances.
     *
     * @param uri the address URI for the endpoint
     * @return and EndpointBuilder instance that can be used to create endpoints
     * @throws MuleException if the builder cannt be created for any reason
     */
    protected EndpointBuilder createEndpointBuilder(String uri) throws MuleException
    {
        DefaultEndpointFactory endpointFactory = new DefaultEndpointFactory();
        endpointFactory.setMuleContext(muleContext);
        return endpointFactory.getEndpointBuilder(uri);
    }

    /**
     * A convenience method that will register an object in the registry using its hashcode as the key.  This will cause the object
     * to have any objects injected and lifecycle methods called.  Note that the object lifecycle will be called to the same current
     * lifecycle as the MuleContext
     *
     * @param o the object to register and initialise it
     * @throws org.mule.api.registry.RegistrationException
     *
     */
    protected void initialiseObject(Object o) throws RegistrationException
    {
        if (o instanceof NamedObject)
        {
            muleContext.getRegistry().registerObject(((NamedObject) o).getName(), o);
        }
        else
        {
            muleContext.getRegistry().registerObject(String.valueOf(o.getClass().getSimpleName() + "#" + o.hashCode()), o);
        }
    }

    /**
     * Used to register any objects created by Guice with the Mule registry, which means all lifecycle, callbacks and
     * annotations will be honoured.
     */
    private class MuleRegistryInjectionLister implements InjectionListener
    {
        public void afterInjection(Object o)
        {
            //We don't need or want to register this object again.
            //Guice doesn't create it, but we bind it in the injector
            if (o instanceof MuleContext)
            {
                return;
            }
            try
            {
                initialiseObject(o);
            }
            catch (Exception e)
            {
                throw new MuleRuntimeException(GuiceMessages.failedToRegisterOBjectWithMule(o.getClass()));
            }
        }
    }
}
