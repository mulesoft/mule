/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.resolver;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.extension.ExtensionManager;
import org.mule.extension.runtime.ExpirationPolicy;
import org.mule.extension.runtime.event.OperationSuccessfulSignal;
import org.mule.module.extension.HeisenbergExtension;
import org.mule.module.extension.internal.runtime.config.ConfigurationObjectBuilder;
import org.mule.module.extension.internal.runtime.config.DynamicConfigurationProvider;
import org.mule.module.extension.internal.runtime.ImmutableExpirationPolicy;
import org.mule.module.extension.internal.util.ExtensionsTestUtils;
import org.mule.tck.size.SmallTest;
import org.mule.tck.util.TestTimeSupplier;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DynamicConfigurationProviderTestCase extends AbstractConfigurationInstanceProviderTestCase
{

    private static final Class MODULE_CLASS = HeisenbergExtension.class;

    @Mock
    private ResolverSet resolverSet;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ResolverSetResult resolverSetResult;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private MuleContext muleContext;

    @Mock
    private MuleEvent event;

    @Mock
    private ExtensionManager extensionManager;

    private ExpirationPolicy expirationPolicy;

    private TestTimeSupplier timeSupplier = new TestTimeSupplier(System.currentTimeMillis());

    private ConfigurationObjectBuilder configurationObjectBuilder;

    @Before
    public void before() throws Exception
    {
        ExtensionsTestUtils.stubRegistryKeys(muleContext, CONFIG_NAME);
        when(configurationModel.getInstantiator().getObjectType()).thenReturn(MODULE_CLASS);
        when(configurationModel.getInstantiator().newInstance()).thenAnswer(invocation -> MODULE_CLASS.newInstance());
        when(configurationModel.getCapabilities(any(Class.class))).thenReturn(null);

        when(resolverSet.resolve(event)).thenReturn(resolverSetResult);
        when(muleContext.getExtensionManager()).thenReturn(extensionManager);

        when(operationContext.getEvent()).thenReturn(event);

        configurationObjectBuilder = new ConfigurationObjectBuilder(configurationModel, resolverSet);
        expirationPolicy = new ImmutableExpirationPolicy(5, TimeUnit.MINUTES, timeSupplier);

        instanceProvider = new DynamicConfigurationProvider(CONFIG_NAME,
                                                                    extensionModel,
                                                            configurationRegistrationCallback,
                                                                    configurationObjectBuilder,
                                                                    resolverSet,
                                                                    expirationPolicy);
    }

    @Test
    public void resolveCached() throws Exception
    {
        final int count = 10;
        HeisenbergExtension config = (HeisenbergExtension) instanceProvider.get(operationContext);
        for (int i = 1; i < count; i++)
        {
            assertThat(instanceProvider.get(operationContext), is(sameInstance(config)));
        }

        verify(resolverSet, times(count)).resolve(event);
    }

    @Test
    public void resolveDifferentInstances() throws Exception
    {
        HeisenbergExtension instance1 = (HeisenbergExtension) instanceProvider.get(operationContext);
        HeisenbergExtension instance2 = makeAlternateInstance();

        assertThat(instance2, is(not(sameInstance(instance1))));
    }

    @Test
    public void getExpired() throws Exception
    {
        final String key1 = "key1";
        final String key2 = "key2";

        when(configurationRegistrationCallback.registerConfiguration(same(extensionModel), same(CONFIG_NAME), any(Object.class)))
                .thenReturn(key1)
                .thenReturn(key2);

        doAnswer(invocation -> {
            ((Consumer<OperationSuccessfulSignal>) invocation.getArguments()[0]).accept(mock(OperationSuccessfulSignal.class));
            return null;
        }).when(operationContext).onOperationSuccessful(any(Consumer.class));

        HeisenbergExtension instance1 = (HeisenbergExtension) instanceProvider.get(operationContext);
        HeisenbergExtension instance2 = makeAlternateInstance();

        DynamicConfigurationProvider provider = (DynamicConfigurationProvider) instanceProvider;
        timeSupplier.move(1, TimeUnit.MINUTES);

        Map<String, Object> expired = provider.getExpired();
        assertThat(expired.isEmpty(), is(true));

        timeSupplier.move(10, TimeUnit.MINUTES);

        expired = provider.getExpired();
        assertThat(expired.isEmpty(), is(false));
        assertThat(expired.get(key1), is(sameInstance(instance1)));
        assertThat(expired.get(key2), is(sameInstance(instance2)));
    }

    private HeisenbergExtension makeAlternateInstance() throws MuleException
    {
        ResolverSetResult alternateResult = mock(ResolverSetResult.class, Mockito.RETURNS_DEEP_STUBS);
        when(resolverSet.resolve(event)).thenReturn(alternateResult);

        return (HeisenbergExtension) instanceProvider.get(operationContext);
    }

    @Test
    public void resolveDynamicConfigWithEquivalentEvent() throws Exception
    {
        assertSameInstancesResolved();
        assertConfigInstanceRegistered(instanceProvider.get(operationContext));
    }

    @Test
    public void resolveDynamicConfigWithDifferentEvent() throws Exception
    {
        Object config1 = instanceProvider.get(operationContext);

        when(resolverSet.resolve(event)).thenReturn(mock(ResolverSetResult.class));
        Object config2 = instanceProvider.get(operationContext);

        assertThat(config1, is(not(Matchers.sameInstance(config2))));
        assertConfigInstanceRegistered(config1);
        assertConfigInstanceRegistered(config2);
    }
}
