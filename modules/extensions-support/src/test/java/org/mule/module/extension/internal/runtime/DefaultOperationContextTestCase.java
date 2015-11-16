/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import org.mule.api.MuleEvent;
import org.mule.extension.api.ExtensionManager;
import org.mule.extension.api.introspection.ConfigurationModel;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.OperationModel;
import org.mule.extension.api.introspection.ParameterModel;
import org.mule.extension.api.runtime.ConfigurationInstance;
import org.mule.module.extension.internal.runtime.config.LifecycleAwareConfigurationInstance;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.module.extension.internal.util.ExtensionsTestUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultOperationContextTestCase extends AbstractMuleTestCase
{

    private static final String CONFIG_NAME = "config";
    private static final String PARAM_NAME = "param1";
    private static final String VALUE = "Do you want to build a snowman?";

    @Mock
    private ExtensionModel extensionModel;

    @Mock
    private ConfigurationModel configurationModel;

    @Mock
    private OperationModel operationModel;

    @Mock
    private ResolverSetResult resolverSetResult;

    @Mock
    private MuleEvent event;

    @Mock
    private ExtensionManager extensionManager;

    private Object configurationInstance = new Object();
    private ConfigurationInstance<Object> configuration;
    private DefaultOperationContext operationContext;


    @Before
    public void before()
    {
        configuration = new LifecycleAwareConfigurationInstance<>(CONFIG_NAME, configurationModel, configurationInstance, emptyList(), Optional.empty());
        Map<ParameterModel, Object> parametersMap = new HashMap<>();
        parametersMap.put(ExtensionsTestUtils.getParameter(PARAM_NAME, String.class), VALUE);
        when(resolverSetResult.asMap()).thenReturn(parametersMap);

        operationContext = new DefaultOperationContext(configuration, resolverSetResult, operationModel, event);
    }

    @Test
    public void getParameter()
    {
        assertThat(operationContext.getParameter(PARAM_NAME), is(VALUE));
    }

    @Test
    public void getTypeSafeParameter()
    {
        assertThat(operationContext.getTypeSafeParameter(PARAM_NAME, String.class), is(VALUE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getTypeSafeParameterFails()
    {
        operationContext.getTypeSafeParameter(PARAM_NAME, Integer.class);
    }

    @Test
    public void variables()
    {
        final String key = "foo";
        final String value = "bar";

        assertThat(operationContext.getVariable(key), is(nullValue()));
        assertThat(operationContext.setVariable(key, value), is(nullValue()));
        assertThat(operationContext.getVariable(key), is(value));
        assertThat(operationContext.setVariable(key, EMPTY), is(value));
        assertThat(operationContext.getVariable(key), is(EMPTY));
        assertThat(operationContext.removeVariable(key), is(EMPTY));
        assertThat(operationContext.getVariable(key), is(nullValue()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void setNullKeyVariable()
    {
        operationContext.setVariable(null, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setNullValueVariable()
    {
        operationContext.setVariable("key", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeNullValueVariable()
    {
        operationContext.removeVariable(null);
    }
}
