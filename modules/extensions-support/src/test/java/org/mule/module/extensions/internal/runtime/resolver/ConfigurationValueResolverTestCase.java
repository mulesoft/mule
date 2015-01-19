/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.runtime.resolver;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.module.extensions.internal.util.ExtensionsTestUtils.getParameter;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.extensions.introspection.Configuration;
import org.mule.extensions.introspection.Parameter;
import org.mule.module.extensions.HeisenbergExtension;
import org.mule.tck.junit4.AbstractMuleTestCase;
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

    @Mock
    private MuleContext muleContext;

    @Mock
    private MuleEvent event;

    private HeisenbergExtension config;

    @Mock
    private ResolverSetResult resolverSetResult = mock(ResolverSetResult.class);

    private ConfigurationValueResolver resolver;

    @Before
    public void before() throws Exception
    {
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
    }

    @Test
    public void resolveDynamicConfig() throws Exception
    {
        resolver = getDynamicConfigResolver();
        assertSameInstancesResolved();
    }

    private void assertSameInstancesResolved() throws Exception
    {
        final int count = 10;
        Object config = resolver.resolve(event);

        for (int i = 1; i < count; i++)
        {
            assertThat(resolver.resolve(event), is(sameInstance(config)));
        }
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

    @Test
    public void staticConfigInitialisation() throws Exception
    {
        resolver = getStaticConfigResolver();
        assertInitialisation();
    }

    @Test
    public void dynamicConfigInitialisation() throws Exception
    {
        resolver = getDynamicConfigResolver();
        assertInitialisation();
    }

    @Test
    public void staticConfigStart() throws Exception
    {
        resolver = getStaticConfigResolver();
        assertStarted();
    }

    @Test
    public void dynamicConfigStart() throws Exception
    {
        resolver = getDynamicConfigResolver();
        assertStarted();
    }

    @Test
    public void staticConfigStop() throws Exception
    {
        resolver = getStaticConfigResolver();
        assertStopped();
    }

    @Test
    public void dynamicConfigStop() throws Exception
    {
        resolver = getDynamicConfigResolver();
        assertStopped();
    }

    @Test
    public void staticConfigDispose() throws Exception
    {
        resolver = getStaticConfigResolver();
        assertDisposed();
    }

    @Test
    public void dynamicConfigDispose() throws Exception
    {
        resolver = getDynamicConfigResolver();
        assertDisposed();
    }

    private void assertInitialisation() throws Exception
    {
        config = (HeisenbergExtension) resolver.resolve(event);
        assertThat(config.getMuleContext(), is(sameInstance(muleContext)));
        assertThat(config.getInitialise(), is(1));
    }

    private void assertStopped() throws Exception
    {
        config = (HeisenbergExtension) resolver.resolve(event);

        resolver.stop();
        verify(resolverSet).stop();
        assertThat(config.getStop(), is(1));
    }

    private void assertDisposed() throws Exception
    {
        config = (HeisenbergExtension) resolver.resolve(event);

        resolver.dispose();
        verify(resolverSet).dispose();
        assertThat(config.getDispose(), is(1));
    }

    private void assertStarted() throws Exception
    {
        config = (HeisenbergExtension) resolver.resolve(event);
        verify(resolverSet).start();
        assertThat(config.getStart(), is(1));
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
        ConfigurationValueResolver resolver = new ConfigurationValueResolver(CONFIG_NAME, configuration, resolverSet);
        resolver.setMuleContext(muleContext);
        resolver.initialise();
        resolver.start();

        return resolver;
    }
}
