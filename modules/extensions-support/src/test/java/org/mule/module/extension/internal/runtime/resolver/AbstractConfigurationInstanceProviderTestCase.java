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
import static org.mockito.Mockito.verify;
import org.mule.extension.runtime.ConfigurationInstanceProvider;
import org.mule.extension.runtime.ConfigurationInstanceRegistrationCallback;
import org.mule.module.extension.internal.runtime.DefaultOperationContext;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.mockito.Mock;

abstract class AbstractConfigurationInstanceProviderTestCase extends AbstractMuleTestCase
{
    @Mock
    protected DefaultOperationContext operationContext;

    @Mock
    protected ConfigurationInstanceRegistrationCallback configurationInstanceRegistrationCallback;

    protected ConfigurationInstanceProvider<Object> instanceProvider;


    protected <T> void assertConfigInstanceRegistered(ConfigurationInstanceProvider<T> configurationInstanceProvider, T configurationInstance)
    {
        verify(configurationInstanceRegistrationCallback).registerNewConfigurationInstance(configurationInstanceProvider, configurationInstance);
    }

    protected void assertSameInstancesResolved() throws Exception
    {
        final int count = 10;
        Object config = instanceProvider.get(operationContext, configurationInstanceRegistrationCallback);

        for (int i = 1; i < count; i++)
        {
            assertThat(instanceProvider.get(operationContext, configurationInstanceRegistrationCallback), is(sameInstance(config)));
        }

        assertConfigInstanceRegistered(instanceProvider, config);
    }
}
