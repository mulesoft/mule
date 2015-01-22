/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.runtime.resolver;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.MuleEvent;
import org.mule.extensions.introspection.Configuration;
import org.mule.module.extensions.HeisenbergExtension;
import org.mule.module.extensions.internal.runtime.ConfigurationObjectBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class CachedConfigurationValueResolverTestCase extends AbstractMuleTestCase
{

    private static final Class MODULE_CLASS = HeisenbergExtension.class;
    private static final long EXPIRATION_INTERVAL = 500;
    private static final TimeUnit EXPIRATION_TIME_UNIT = TimeUnit.MILLISECONDS;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private Configuration configuration;

    @Mock
    private ResolverSet resolverSet;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ResolverSetResult resolverSetResult;

    @Mock
    private MuleEvent event;

    private CachedConfigurationValueResolver resolver;

    private ConfigurationObjectBuilder configurationObjectBuilder;

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

        when(resolverSet.resolve(event)).thenReturn(resolverSetResult);
        configurationObjectBuilder = new ConfigurationObjectBuilder(configuration, resolverSet);
        resolver = new CachedConfigurationValueResolver(configurationObjectBuilder, resolverSet, EXPIRATION_INTERVAL, EXPIRATION_TIME_UNIT);
    }

    @Test
    public void resolveCached() throws Exception
    {
        final int count = 10;
        HeisenbergExtension config = (HeisenbergExtension) resolver.resolve(event);
        for (int i = 1; i < count; i++)
        {
            assertThat((HeisenbergExtension) resolver.resolve(event), is(sameInstance(config)));
        }

        verify(resolverSet, times(count)).resolve(event);
    }

    @Test
    public void resolveDifferentInstances() throws Exception
    {
        HeisenbergExtension instance1 = (HeisenbergExtension) resolver.resolve(event);

        ResolverSetResult alternateResult = mock(ResolverSetResult.class, Mockito.RETURNS_DEEP_STUBS);
        when(resolverSet.resolve(event)).thenReturn(alternateResult);

        HeisenbergExtension instance2 = (HeisenbergExtension) resolver.resolve(event);
        assertThat(instance2, is(not(sameInstance(instance1))));
    }

    @Test
    public void stop() throws Exception
    {
        HeisenbergExtension config = (HeisenbergExtension) resolver.resolve(event);
        resolver.stop();
        assertThat(config.getStop(), is(1));
    }

    @Test
    public void dispose() throws Exception
    {
        HeisenbergExtension config = (HeisenbergExtension) resolver.resolve(event);
        resolver.dispose();
        assertThat(config.getDispose(), is(1));
    }

    @Test
    public void timeBasedEviction() throws Exception
    {
        HeisenbergExtension config = (HeisenbergExtension) resolver.resolve(event);

        Thread.sleep(EXPIRATION_INTERVAL);
        resolver.cleanUpCache();

        assertThat(config.getStop(), is(1));
        assertThat(config.getDispose(), is(1));
    }

}
