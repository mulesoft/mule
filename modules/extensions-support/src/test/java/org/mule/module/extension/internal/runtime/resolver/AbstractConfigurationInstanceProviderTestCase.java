/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.resolver;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.verify;
import org.mule.extension.introspection.ConfigurationModel;
import org.mule.extension.introspection.ExtensionModel;
import org.mule.extension.runtime.ConfigurationProvider;
import org.mule.extension.runtime.ConfigurationRegistrationCallback;
import org.mule.module.extension.internal.runtime.DefaultOperationContext;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.mockito.Mock;

abstract class AbstractConfigurationInstanceProviderTestCase extends AbstractMuleTestCase
{

    protected static final String CONFIG_NAME = "config";

    @Mock
    protected ExtensionModel extensionModel;

    @Mock(answer = RETURNS_DEEP_STUBS)
    protected ConfigurationModel configurationModel;

    @Mock
    protected DefaultOperationContext operationContext;

    @Mock
    protected ConfigurationRegistrationCallback configurationRegistrationCallback;

    protected ConfigurationProvider<Object> provider;

    @Test
    public void getName()
    {
        assertThat(provider.getName(), is(CONFIG_NAME));
    }

    @Test
    public void getConfigurationModel()
    {
        assertThat(provider.getModel(), is(CoreMatchers.sameInstance(configurationModel)));
    }

    protected <T> void assertConfigInstanceRegistered(T configurationInstance)
    {
        verify(configurationRegistrationCallback).registerConfiguration(extensionModel, CONFIG_NAME, configurationInstance);
    }

    protected void assertSameInstancesResolved() throws Exception
    {
        final int count = 10;
        Object config = provider.get(operationContext);

        for (int i = 1; i < count; i++)
        {
            assertThat(provider.get(operationContext), is(sameInstance(config)));
        }
    }
}
