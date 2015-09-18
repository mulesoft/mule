/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.MuleException;
import org.mule.api.extension.runtime.ConfigurationInstance;
import org.mule.api.extension.runtime.ExpirationPolicy;
import org.mule.module.extension.HeisenbergExtension;
import org.mule.module.extension.internal.runtime.ImmutableExpirationPolicy;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.tck.size.SmallTest;
import org.mule.util.collection.ImmutableListCollector;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DynamicConfigurationProviderTestCase extends AbstractConfigurationProviderTestCase<HeisenbergExtension>
{

    private static final Class MODULE_CLASS = HeisenbergExtension.class;

    @Mock
    private ResolverSet resolverSet;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ResolverSetResult resolverSetResult;

    private ExpirationPolicy expirationPolicy;

    @Before
    public void before() throws Exception
    {
        when(configurationModel.getInstantiator().getObjectType()).thenReturn(MODULE_CLASS);
        when(configurationModel.getInstantiator().newInstance()).thenAnswer(invocation -> MODULE_CLASS.newInstance());
        when(configurationModel.getModelProperty(anyString())).thenReturn(null);
        when(configurationModel.getInterceptorFactories()).thenReturn(ImmutableList.of());

        when(resolverSet.resolve(event)).thenReturn(resolverSetResult);


        expirationPolicy = new ImmutableExpirationPolicy(5, TimeUnit.MINUTES, timeSupplier);

        provider = new DynamicConfigurationProvider(CONFIG_NAME,
                                                    configurationModel,
                                                    resolverSet,
                                                    expirationPolicy);

        super.before();
        provider.initialise();
        provider.start();
    }

    @Test
    public void resolveCached() throws Exception
    {
        final int count = 10;
        HeisenbergExtension config = provider.get(event).getValue();
        for (int i = 1; i < count; i++)
        {
            assertThat(provider.get(event).getValue(), is(sameInstance(config)));
        }

        verify(resolverSet, times(count)).resolve(event);
    }

    @Test
    public void resolveDifferentInstances() throws Exception
    {
        HeisenbergExtension instance1 = provider.get(event).getValue();
        HeisenbergExtension instance2 = makeAlternateInstance();

        assertThat(instance2, is(not(sameInstance(instance1))));
    }

    @Test
    public void getExpired() throws Exception
    {
        HeisenbergExtension instance1 = provider.get(event).getValue();
        HeisenbergExtension instance2 = makeAlternateInstance();

        DynamicConfigurationProvider provider = (DynamicConfigurationProvider) this.provider;
        timeSupplier.move(1, TimeUnit.MINUTES);

        List<ConfigurationInstance<Object>> expired = provider.getExpired();
        assertThat(expired.isEmpty(), is(true));

        timeSupplier.move(10, TimeUnit.MINUTES);

        expired = provider.getExpired();
        assertThat(expired.isEmpty(), is(false));

        List<Object> configs = expired.stream().map(config -> config.getValue()).collect(new ImmutableListCollector<>());
        assertThat(configs, containsInAnyOrder(instance1, instance2));
    }

    private HeisenbergExtension makeAlternateInstance() throws MuleException
    {
        ResolverSetResult alternateResult = mock(ResolverSetResult.class, Mockito.RETURNS_DEEP_STUBS);
        when(resolverSet.resolve(event)).thenReturn(alternateResult);

        return provider.get(event).getValue();
    }

    @Test
    public void resolveDynamicConfigWithEquivalentEvent() throws Exception
    {
        assertSameInstancesResolved();
    }

    @Test
    public void resolveDynamicConfigWithDifferentEvent() throws Exception
    {
        Object config1 = provider.get(event);

        when(resolverSet.resolve(event)).thenReturn(mock(ResolverSetResult.class));
        Object config2 = provider.get(event);

        assertThat(config1, is(not(sameInstance(config2))));
    }
}
