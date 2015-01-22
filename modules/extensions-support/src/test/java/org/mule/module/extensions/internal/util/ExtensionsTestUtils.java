/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.util;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.extensions.introspection.DataType;
import org.mule.extensions.introspection.Parameter;
import org.mule.module.extensions.internal.runtime.resolver.ValueResolver;

import java.util.Collection;

import org.apache.commons.lang.ArrayUtils;
import org.mockito.InOrder;

public abstract class ExtensionsTestUtils
{

    public static final String HELLO_WORLD = "Hello World!";

    public static ValueResolver getResolver(Object value, MuleEvent event, boolean dynamic, Class<?>... extraInterfaces) throws Exception
    {
        ValueResolver resolver;
        if (ArrayUtils.isEmpty(extraInterfaces))
        {
            resolver = mock(ValueResolver.class);
        }
        else
        {
            resolver = mock(ValueResolver.class, withSettings().extraInterfaces(extraInterfaces));
        }

        when(resolver.resolve(event)).thenReturn(value);
        when(resolver.isDynamic()).thenReturn(dynamic);

        return resolver;
    }

    public static Parameter getParameter(String name, Class<?> type)
    {
        Parameter parameter = mock(Parameter.class);
        when(parameter.getName()).thenReturn(name);
        when(parameter.getType()).thenReturn(DataType.of(type));

        return parameter;
    }

    public static void verifyInitialisation(Object object, MuleContext muleContext) throws Exception
    {
        InOrder order = inOrder(object);
        order.verify((MuleContextAware) object).setMuleContext(muleContext);
        order.verify((Initialisable) object).initialise();
    }

    public static void verifyAllInitialised(Collection<? extends Object> objects, MuleContext muleContext) throws Exception
    {
        for (Object object : objects)
        {
            verifyInitialisation(object, muleContext);
        }
    }

    public static void verifyAllStarted(Collection<? extends Object> objects) throws Exception
    {
        for (Object object : objects)
        {
            verify((Startable) object).start();
        }
    }

    public static void verifyAllStopped(Collection<? extends Object> objects) throws Exception
    {
        for (Object object : objects)
        {
            verify((Stoppable) object).stop();
        }
    }

    public static void verifyAllDisposed(Collection<? extends Object> objects) throws Exception
    {
        for (Object object : objects)
        {
            verify((Disposable) object).dispose();
        }
    }

}
