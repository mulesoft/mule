/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.getParameter;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.extension.api.runtime.ExpirationPolicy;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.runtime.module.extension.internal.runtime.resolver.StaticValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.HeisenbergConnectionProvider;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import com.google.common.collect.ImmutableList;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class StaticConfigurationProviderTestCase extends AbstractConfigurationProviderTestCase<HeisenbergExtension>
{

    private static final Class MODULE_CLASS = HeisenbergExtension.class;
    private static final String MY_NAME = "heisenberg";
    private static final int AGE = 50;

    @Mock
    private ResolverSet resolverSet;

    @Mock
    private MuleEvent event;

    @Mock
    private ResolverSetResult resolverSetResult;

    @Mock
    private ExpirationPolicy expirationPolicy;

    private ConnectionProvider connectionProvider = new HeisenbergConnectionProvider();

    @Before
    public void before() throws Exception
    {
        when(configurationModel.getConfigurationFactory().getObjectType()).thenReturn(MODULE_CLASS);
        when(configurationModel.getConfigurationFactory().newInstance()).thenAnswer(invocation -> MODULE_CLASS.newInstance());
        when(configurationModel.getModelProperty(any())).thenReturn(Optional.empty());
        when(configurationModel.getExtensionModel()).thenReturn(extensionModel);
        when(configurationModel.getInterceptorFactories()).thenReturn(ImmutableList.of());
        when(configurationModel.getOperationModels()).thenReturn(ImmutableList.of());

        when(operationContext.getEvent()).thenReturn(event);

        Map<ParameterModel, ValueResolver> parameters = new HashMap<>();
        parameters.put(getParameter("myName", String.class), new StaticValueResolver(MY_NAME));
        parameters.put(getParameter("age", Integer.class), new StaticValueResolver(AGE));
        when(resolverSet.getResolvers()).thenReturn(parameters);
        when(resolverSet.isDynamic()).thenReturn(false);

        provider = (LifecycleAwareConfigurationProvider) new DefaultConfigurationProviderFactory().createStaticConfigurationProvider(CONFIG_NAME,
                                                                                                                                     configurationModel,
                                                                                                                                     resolverSet,
                                                                                                                                     new StaticValueResolver<>(connectionProvider),
                                                                                                                                     muleContext);
        super.before();
    }

    @Test
    public void resolveStaticConfig() throws Exception
    {
        assertSameInstancesResolved();
    }

    @Test
    public void getConnectionProvider()
    {
        assertThat(provider.get(event).getConnectionProvider().get(), is(sameInstance(connectionProvider)));
    }

    @Test
    public void initialise() throws Exception
    {
        provider.initialise();
        HeisenbergExtension config = provider.get(operationContext).getValue();
        verify(muleContext.getInjector()).inject(config);
        assertLifecycle(HeisenbergExtension::getInitialise);
    }

    @Test
    public void start() throws Exception
    {
        provider.initialise();
        provider.start();
        assertLifecycle(HeisenbergExtension::getStart);
    }

    @Test
    public void stop() throws Exception
    {
        provider.initialise();
        provider.start();
        provider.stop();
        assertLifecycle(HeisenbergExtension::getStop);
    }

    @Test
    public void dispose() throws Exception
    {
        provider.initialise();
        provider.start();
        provider.stop();
        provider.dispose();
        assertLifecycle(HeisenbergExtension::getDispose);
    }


    private void assertLifecycle(Function<HeisenbergExtension, Integer> testFunction)
    {
        HeisenbergExtension config = provider.get(operationContext).getValue();
        assertThat(testFunction.apply(config), is(1));
    }
}
