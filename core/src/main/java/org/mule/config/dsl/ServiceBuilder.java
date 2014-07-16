/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.dsl;

import org.mule.api.MuleContext;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.service.Service;
import org.mule.model.seda.SedaService;

/**
 * TODO
 */
@Deprecated
public class ServiceBuilder
{
    private Service service;
    private MuleContext muleContext;


    public ServiceBuilder(String name, MuleContext muleContext)
    {
        this.muleContext = muleContext;
        service = new SedaService(muleContext);
        service.setName(name);
    }

    public ComponentBuilder toComponent(Class clazz)
    {
        ComponentBuilder builder = new ComponentBuilder(ComponentBuilder.Scope.Prototype, clazz, muleContext);
        service.setComponent(builder.create());
        return builder;
    }

    public ComponentBuilder toPooledComponent(Class clazz)
    {
        ComponentBuilder builder = new ComponentBuilder(ComponentBuilder.Scope.Pooled, clazz, muleContext);
        service.setComponent(builder.create());
        return builder;
    }

    public ComponentBuilder toComponent(Object instance)
    {
        ComponentBuilder builder = new ComponentBuilder(instance, muleContext);
        service.setComponent(builder.create());
        return builder;
    }

    public OutRouteBuilder to(String uri)
    {
        OutRouteBuilder rb = new OutRouteBuilder(
            (OutboundRouterCollection) service.getOutboundMessageProcessor(), muleContext);
        rb.to(uri);
        return rb;
    }

    Service create()
    {
        return service;
    }
}
