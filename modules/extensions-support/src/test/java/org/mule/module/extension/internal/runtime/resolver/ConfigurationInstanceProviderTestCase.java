/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.resolver;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.mule.module.extension.internal.util.ExtensionsTestUtils.getParameter;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.extension.introspection.Parameter;
import org.mule.module.extension.HeisenbergExtension;
import org.mule.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.module.extension.internal.util.ExtensionsTestUtils;
import org.mule.module.extension.internal.util.MuleExtensionUtils;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ConfigurationInstanceProviderTestCase extends AbstractConfigurationInstanceProviderTestCase
{
    private static final Class MODULE_CLASS = HeisenbergExtension.class;
    private static final String MY_NAME = "heisenberg";
    private static final int AGE = 50;

    @Mock
    private ResolverSet resolverSet;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private MuleContext muleContext;

    @Mock
    private MuleEvent event;

    @Mock
    private ResolverSetResult resolverSetResult;

    @Mock
    private ExtensionManagerAdapter extensionManager;

    @Before
    public void before() throws Exception
    {
        when(muleContext.getExtensionManager()).thenReturn(extensionManager);
        ExtensionsTestUtils.stubRegistryKeys(muleContext, CONFIG_NAME);

        when(configuration.getInstantiator().getObjectType()).thenReturn(MODULE_CLASS);
        when(configuration.getInstantiator().newInstance()).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                return MODULE_CLASS.newInstance();
            }
        });
        when(configuration.getCapabilities(any(Class.class))).thenReturn(null);

        when(operationContext.getEvent()).thenReturn(event);

        Map<Parameter, ValueResolver> parameters = new HashMap<>();
        parameters.put(getParameter("myName", String.class), new StaticValueResolver(MY_NAME));
        parameters.put(getParameter("age", Integer.class), new StaticValueResolver(AGE));
        when(resolverSet.getResolvers()).thenReturn(parameters);
        when(resolverSet.isDynamic()).thenReturn(false);

        instanceProvider = MuleExtensionUtils.createConfigurationInstanceProvider(CONFIG_NAME, extension, configuration, resolverSet, muleContext, extensionManager);
    }

    @Test
    public void resolveStaticConfig() throws Exception
    {
        assertSameInstancesResolved();
    }

}
