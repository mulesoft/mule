/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.config;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.MuleEvent;
import org.mule.api.extension.introspection.ConfigurationModel;
import org.mule.api.extension.introspection.Interceptable;
import org.mule.api.extension.runtime.ConfigurationInstance;
import org.mule.api.extension.runtime.Interceptor;
import org.mule.module.extension.internal.model.property.ParameterGroupModelProperty;
import org.mule.module.extension.internal.runtime.executor.ConfigurationObjectBuilderTestCase;
import org.mule.module.extension.internal.runtime.executor.ConfigurationObjectBuilderTestCase.TestConfig;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ConfigurationInstanceFactoryTestCase extends AbstractMuleTestCase
{

    private static final String CONFIG_NAME = "config";

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ConfigurationModel configurationModel;

    @Mock
    private Interceptor interceptor1;

    @Mock
    private Interceptor interceptor2;

    private ResolverSet resolverSet;
    private ConfigurationInstanceFactory<TestConfig> factory;

    @Before
    public void before() throws Exception
    {
        when(configurationModel.getInstantiator().newInstance()).thenReturn(new TestConfig());
        when(configurationModel.getModelProperty(ParameterGroupModelProperty.KEY)).thenReturn(null);
        when(configurationModel.getInterceptorFactories()).thenReturn(asList(() -> interceptor1, () -> interceptor2));

        resolverSet = ConfigurationObjectBuilderTestCase.createResolverSet();
        factory = new ConfigurationInstanceFactory<>(configurationModel, resolverSet);
    }

    @Test
    public void createFromEvent() throws Exception
    {
        MuleEvent event = mock(MuleEvent.class);
        ConfigurationInstance<TestConfig> configurationInstance = factory.createConfiguration(CONFIG_NAME, event);

        assertConfiguration(configurationInstance);
    }

    @Test
    public void createFromResolverSetResult() throws Exception
    {
        ResolverSetResult result = ResolverSetResult.newBuilder().build();
        ConfigurationInstance<TestConfig> configurationInstance = factory.createConfiguration(CONFIG_NAME, result);

        assertConfiguration(configurationInstance);
    }

    private void assertConfiguration(ConfigurationInstance<TestConfig> configurationInstance)
    {
        assertThat(configurationInstance, is(notNullValue()));
        assertThat(configurationInstance.getName(), is(CONFIG_NAME));
        assertThat(configurationInstance.getModel(), is(sameInstance(configurationModel)));
        assertThat(configurationInstance.getValue(), is(instanceOf(TestConfig.class)));

        assertThat(configurationInstance, is(instanceOf(Interceptable.class)));
        assertThat(((Interceptable) configurationInstance).getInterceptors(), containsInAnyOrder(interceptor1, interceptor2));
    }
}
