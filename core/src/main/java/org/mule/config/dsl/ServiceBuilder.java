/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.dsl;

import org.mule.api.MuleContext;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.service.Service;
import org.mule.model.seda.SedaService;

/**
 * TODO
 */
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
