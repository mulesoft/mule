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
import org.mule.extension.introspection.Configuration;
import org.mule.extension.introspection.Extension;
import org.mule.extension.runtime.ConfigurationInstanceProvider;
import org.mule.extension.runtime.ConfigurationInstanceRegistrationCallback;
import org.mule.module.extension.internal.runtime.DefaultOperationContext;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.mockito.Mock;

abstract class AbstractConfigurationInstanceProviderTestCase extends AbstractMuleTestCase
{

    protected static final String CONFIG_NAME = "config";

    @Mock
    protected Extension extension;

    @Mock(answer = RETURNS_DEEP_STUBS)
    protected Configuration configuration;

    @Mock
    protected DefaultOperationContext operationContext;

    @Mock
    protected ConfigurationInstanceRegistrationCallback configurationInstanceRegistrationCallback;

    protected ConfigurationInstanceProvider<Object> instanceProvider;


    protected <T> void assertConfigInstanceRegistered(T configurationInstance)
    {
        verify(configurationInstanceRegistrationCallback).registerConfigurationInstance(extension, CONFIG_NAME, configurationInstance);
    }

    protected void assertSameInstancesResolved() throws Exception
    {
        final int count = 10;
        Object config = instanceProvider.get(operationContext);

        for (int i = 1; i < count; i++)
        {
            assertThat(instanceProvider.get(operationContext), is(sameInstance(config)));
        }
    }
}
