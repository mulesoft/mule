/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.guice;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.endpoint.DefaultEndpointFactory;

import com.google.inject.AbstractModule;

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
        // do nothing
    }

    /**
     * Creates an {@link org.mule.api.endpoint.EndpointBuilder} instance for the endpoint uri.  The builder can be used to add
     * further configuration options and then used to create either {@link org.mule.api.endpoint.OutboundEndpoint} or
     * {@link org.mule.api.endpoint.InboundEndpoint} instances.
     *
     * @param uri the address URI for the endpoint
     * @return and EndpointBuilder instance that can be used to create endpoints
     * @throws MuleException if the builder cannot be created for any reason
     */
    protected EndpointBuilder createEndpointBuilder(String uri) throws MuleException
    {
        DefaultEndpointFactory endpointFactory = new DefaultEndpointFactory();
        endpointFactory.setMuleContext(muleContext);
        return endpointFactory.getEndpointBuilder(uri);
    }
}
