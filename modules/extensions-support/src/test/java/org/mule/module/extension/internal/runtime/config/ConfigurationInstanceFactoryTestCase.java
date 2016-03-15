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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import org.mule.api.MuleEvent;
import org.mule.api.connection.ConnectionException;
import org.mule.api.connection.ConnectionHandlingStrategy;
import org.mule.api.connection.ConnectionHandlingStrategyFactory;
import org.mule.api.connection.ConnectionProvider;
import org.mule.api.connection.ConnectionValidationResult;
import org.mule.extension.api.introspection.Interceptable;
import org.mule.extension.api.introspection.OperationModel;
import org.mule.extension.api.introspection.RuntimeConfigurationModel;
import org.mule.extension.api.runtime.ConfigurationInstance;
import org.mule.extension.api.runtime.Interceptor;
import org.mule.module.extension.internal.model.property.ConnectionTypeModelProperty;
import org.mule.module.extension.internal.runtime.executor.ConfigurationObjectBuilderTestCase;
import org.mule.module.extension.internal.runtime.executor.ConfigurationObjectBuilderTestCase.TestConfig;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Kiwi;

import java.util.Optional;

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
    private RuntimeConfigurationModel configurationModel;

    @Mock
    private OperationModel operationModel;

    @Mock
    private Interceptor interceptor1;

    @Mock
    private Interceptor interceptor2;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private MuleEvent event;

    private ResolverSet resolverSet;
    private ConfigurationInstanceFactory<TestConfig> factory;

    @Before
    public void before() throws Exception
    {
        when(configurationModel.getConfigurationFactory().newInstance()).thenReturn(new TestConfig());
        when(configurationModel.getModelProperty(any())).thenReturn(Optional.empty());
        when(configurationModel.getInterceptorFactories()).thenReturn(asList(() -> interceptor1, () -> interceptor2));
        when(configurationModel.getExtensionModel().getOperationModels()).thenReturn(asList(operationModel));
        when(operationModel.getModelProperty(ConnectionTypeModelProperty.class)).thenReturn(Optional.of(new ConnectionTypeModelProperty(Banana.class)));

        resolverSet = ConfigurationObjectBuilderTestCase.createResolverSet();
        factory = new ConfigurationInstanceFactory<>(configurationModel, resolverSet);
    }

    @Test
    public void createFromEvent() throws Exception
    {
        ConfigurationInstance<TestConfig> configurationInstance = factory.createConfiguration(CONFIG_NAME, event, new StaticValueResolver<>(null));

        assertConfiguration(configurationInstance);
    }

    @Test
    public void createFromResolverSetResult() throws Exception
    {
        ResolverSetResult result = ResolverSetResult.newBuilder().build();
        ConfigurationInstance<TestConfig> configurationInstance = factory.createConfiguration(CONFIG_NAME, result, Optional.empty());

        assertConfiguration(configurationInstance);
        assertThat(configurationInstance.getConnectionProvider().isPresent(), is(false));
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

    public static class InvalidConfigTestConnectionProvider implements ConnectionProvider<Apple, Banana>
    {

        @Override
        public Banana connect(Apple apple) throws ConnectionException
        {
            return new Banana();
        }

        @Override
        public void disconnect(Banana banana)
        {

        }

        @Override
        public ConnectionValidationResult validate(Banana banana)
        {
            return ConnectionValidationResult.success();
        }

        @Override
        public ConnectionHandlingStrategy<Banana> getHandlingStrategy(ConnectionHandlingStrategyFactory<Apple, Banana> handlingStrategyFactory)
        {
            return handlingStrategyFactory.cached();
        }
    }

    public static class InvalidConnectionTypeProvider implements ConnectionProvider<TestConfig, Kiwi>
    {

        @Override
        public Kiwi connect(TestConfig testConfig) throws ConnectionException
        {
            return new Kiwi();
        }

        @Override
        public void disconnect(Kiwi kiwi)
        {

        }

        @Override
        public ConnectionValidationResult validate(Kiwi kiwi)
        {
            return ConnectionValidationResult.success();
        }

        @Override
        public ConnectionHandlingStrategy<Kiwi> getHandlingStrategy(ConnectionHandlingStrategyFactory<TestConfig, Kiwi> handlingStrategyFactory)
        {
            return handlingStrategyFactory.cached();
        }
    }
}
