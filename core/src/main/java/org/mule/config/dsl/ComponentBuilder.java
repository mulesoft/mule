/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.dsl;

import org.mule.api.component.Component;
import org.mule.api.object.ObjectFactory;
import org.mule.api.MuleContext;
import org.mule.object.PrototypeObjectFactory;
import org.mule.object.SingletonObjectFactory;
import org.mule.component.PooledJavaComponent;
import org.mule.component.DefaultJavaComponent;

/**
 * TODO
 */
public class ComponentBuilder
{
    public enum Scope
    {
        Prototype,
        Pooled,
        Singleton;
    }

    private Component component;
    private MuleContext muleContext;

    public ComponentBuilder(Scope scope, Class clazz, MuleContext muleContext)
    {
        this.muleContext = muleContext;
        ObjectFactory factory;
        if (scope == Scope.Singleton)
        {
            factory = new SingletonObjectFactory(clazz);
        }
        else
        {
            factory = new PrototypeObjectFactory(clazz);
        }

        if (scope == Scope.Pooled)
        {
            component = new PooledJavaComponent(factory);
        }
        else
        {
            component = new DefaultJavaComponent(factory);
        }
    }

    public ComponentBuilder(Object instance, MuleContext muleContext)
    {
        this.muleContext = muleContext;
        ObjectFactory  factory = new SingletonObjectFactory(instance);
        component = new DefaultJavaComponent(factory);
    }

    public RouteBuilder to(String uri)
    {
        return null;
    }

    Component getComponent()
    {
        return component;
    }
}
