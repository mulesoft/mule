/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.resolver;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.module.extension.internal.util.ExtensionsTestUtils.getParameter;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.extension.ExtensionManager;
import org.mule.extension.introspection.Configuration;
import org.mule.extension.introspection.Parameter;
import org.mule.module.extension.HeisenbergExtension;
import org.mule.module.extension.internal.util.ExtensionsTestUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ConfigurationValueResolverTestCase extends AbstractMuleTestCase
{

    private static final String CONFIG_NAME = "myConfig";
    private static final Class MODULE_CLASS = HeisenbergExtension.class;
    private static final String MY_NAME = "heisenberg";
    private static final int AGE = 50;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private Configuration configuration;

    @Mock
    private ResolverSet resolverSet;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private MuleContext muleContext;

    @Mock
    private MuleEvent event;

    @Mock
    private ResolverSetResult resolverSetResult;

    @Mock
    private ExtensionManager extensionManager;

    private ConfigurationValueResolver resolver;

    @Before
    public void before() throws Exception
    {
        when(muleContext.getExtensionManager()).thenReturn(extensionManager);
        ExtensionsTestUtils.stubRegistryKey(muleContext, CONFIG_NAME);

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

        Map<Parameter, ValueResolver> parameters = new HashMap<>();
        parameters.put(getParameter("myName", String.class), new StaticValueResolver(MY_NAME));
        parameters.put(getParameter("age", Integer.class), new StaticValueResolver(AGE));
        when(resolverSet.getResolvers()).thenReturn(parameters);
    }

    @Test
    public void resolveStaticConfig() throws Exception
    {
        resolver = getStaticConfigResolver();
        assertSameInstancesResolved();
        assertConfigInstanceRegistered(resolver.resolve(event));
    }

    @Test
    public void resolveDynamicConfigWithEquivalentEvent() throws Exception
    {
        resolver = getDynamicConfigResolver();
        assertSameInstancesResolved();
    }

    @Test
    public void resolveDynamicConfigWithDifferentEvent() throws Exception
    {
        resolver = getDynamicConfigResolver();
        Object config1 = resolver.resolve(event);

        when(resolverSet.resolve(event)).thenReturn(mock(ResolverSetResult.class));
        Object config2 = resolver.resolve(event);

        assertThat(config1, is(not(sameInstance(config2))));
        assertConfigInstanceRegistered(config1);
        assertConfigInstanceRegistered(config2);
    }

    private void assertConfigInstanceRegistered(Object instance)
    {
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(extensionManager).registerConfigurationInstance(same(configuration), key.capture(), same(instance));
        assertThat(key.getValue(), containsString(CONFIG_NAME));
    }

    private void assertSameInstancesResolved() throws Exception
    {
        final int count = 10;
        Object config = resolver.resolve(event);

        for (int i = 1; i < count; i++)
        {
            assertThat(resolver.resolve(event), is(sameInstance(config)));
        }

        assertConfigInstanceRegistered(config);
    }

    @Test
    public void staticConfigIsNotDynamic() throws Exception
    {
        resolver = getStaticConfigResolver();
        assertThat(resolver.isDynamic(), is(false));
    }

    @Test
    public void dynamicConfigIsDynamic() throws Exception
    {
        resolver = getDynamicConfigResolver();
        assertThat(resolver.isDynamic(), is(true));
    }

    @Test
    public void staticConfigName() throws Exception
    {
        resolver = getStaticConfigResolver();
        assertThat(resolver.getName(), is(CONFIG_NAME));
    }

    @Test
    public void dynamicConfigName() throws Exception
    {
        resolver = getDynamicConfigResolver();
        assertThat(resolver.getName(), is(CONFIG_NAME));
    }

    private ConfigurationValueResolver getStaticConfigResolver() throws Exception
    {
        when(resolverSet.isDynamic()).thenReturn(false);
        return getConfigResolver();
    }

    private ConfigurationValueResolver getDynamicConfigResolver() throws Exception
    {
        when(resolverSet.isDynamic()).thenReturn(true);
        when(resolverSet.resolve(event)).thenReturn(mock(ResolverSetResult.class));

        return getConfigResolver();
    }

    private ConfigurationValueResolver getConfigResolver() throws Exception
    {
        return new ConfigurationValueResolver(CONFIG_NAME, configuration, resolverSet, muleContext);
    }
}
