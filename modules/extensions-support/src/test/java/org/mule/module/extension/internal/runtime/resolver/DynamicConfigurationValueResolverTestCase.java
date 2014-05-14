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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.extension.ExtensionManager;
import org.mule.extension.introspection.Configuration;
import org.mule.module.extension.HeisenbergExtension;
import org.mule.module.extension.internal.runtime.ConfigurationObjectBuilder;
import org.mule.module.extension.internal.util.ExtensionsTestUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

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
public class DynamicConfigurationValueResolverTestCase extends AbstractMuleTestCase
{

    private static final Class MODULE_CLASS = HeisenbergExtension.class;
    private static final String CONFIGURATION_NAME = "heisenberg";

    @Mock(answer = RETURNS_DEEP_STUBS)
    private Configuration configuration;

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

    private DynamicConfigurationValueResolver resolver;

    private ConfigurationObjectBuilder configurationObjectBuilder;

    @Before
    public void before() throws Exception
    {
        ExtensionsTestUtils.stubRegistryKey(muleContext, CONFIGURATION_NAME);
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
        when(muleContext.getExtensionManager()).thenReturn(extensionManager);

        configurationObjectBuilder = new ConfigurationObjectBuilder(configuration, resolverSet);
        resolver = new DynamicConfigurationValueResolver(CONFIGURATION_NAME, configuration, configurationObjectBuilder, resolverSet, muleContext);
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
}
