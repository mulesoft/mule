/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.util;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.extension.introspection.DataType;
import org.mule.extension.introspection.Extension;
import org.mule.extension.introspection.Operation;
import org.mule.extension.introspection.Parameter;
import org.mule.extension.runtime.ConfigurationInstanceProvider;
import org.mule.extension.runtime.OperationContext;
import org.mule.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.module.extension.internal.runtime.DefaultOperationContext;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.module.extension.internal.runtime.resolver.ValueResolver;

import org.apache.commons.lang.ArrayUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public abstract class ExtensionsTestUtils
{

    public static final String HELLO_WORLD = "Hello World!";

    public static ValueResolver getResolver(Object value) throws Exception
    {
        return getResolver(value, null, true);
    }

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

        when(resolver.resolve(event != null ? same(event) : any(MuleEvent.class))).thenReturn(value);
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

    public static void stubRegistryKeys(MuleContext muleContext, final String... keys)
    {
        when(muleContext.getRegistry().get(anyString())).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                String name = (String) invocation.getArguments()[0];
                if (name != null)
                {
                    for (String key : keys)
                    {
                        if (name.contains(key))
                        {
                            return null;
                        }
                    }
                }

                return RETURNS_DEEP_STUBS.get().answer(invocation);
            }
        });
    }

    public static void assertRegisteredWithUniqueMadeKey(MuleContext muleContext, String key, Object object) throws Exception
    {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(muleContext.getRegistry()).registerObject(captor.capture(), same(object));
        assertThat(captor.getValue(), containsString(key));
    }

    public static <C> C getConfigurationInstanceFromExtensionManager(String key, Extension extension, MuleEvent muleEvent) throws Exception
    {
        return extractExtensionManager(muleEvent).getConfigurationInstance(extension, key, getOperationContext(muleEvent));
    }

    public static <C> C getConfigurationInstanceFromRegistry(String key, MuleEvent muleEvent) throws Exception
    {
        ConfigurationInstanceProvider<C> configurationInstanceProvider = muleEvent.getMuleContext().getRegistry().get(key);
        return configurationInstanceProvider.get(getOperationContext(muleEvent));
    }

    private static ExtensionManagerAdapter extractExtensionManager(MuleEvent muleEvent)
    {
        return (ExtensionManagerAdapter) muleEvent.getMuleContext().getExtensionManager();
    }

    private static OperationContext getOperationContext(MuleEvent event) throws Exception
    {
        return new DefaultOperationContext(mock(Extension.class),
                                           mock(Operation.class),
                                           "",
                                           mock(ResolverSetResult.class),
                                           event,
                                           extractExtensionManager(event));
    }
}
