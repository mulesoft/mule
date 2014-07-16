/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.dsl;

import org.mule.api.MuleContext;
import org.mule.api.component.Component;
import org.mule.api.object.ObjectFactory;
import org.mule.component.DefaultJavaComponent;
import org.mule.component.PooledJavaComponent;
import org.mule.object.AbstractObjectFactory;
import org.mule.object.PrototypeObjectFactory;
import org.mule.object.SingletonObjectFactory;

/**
 * TODO
 */
@Deprecated
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

    ComponentBuilder(Scope scope, Class clazz, MuleContext muleContext)
    {
        this.muleContext = muleContext;
        AbstractObjectFactory factory;
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

    ComponentBuilder(Object instance, MuleContext muleContext)
    {
        this.muleContext = muleContext;
        ObjectFactory  factory = new SingletonObjectFactory(instance);
        component = new DefaultJavaComponent(factory);
    }

    Component create()
    {
        return component;
    }

    public OutRouteBuilder to(String uri)
    {
        return null;
    }
}
