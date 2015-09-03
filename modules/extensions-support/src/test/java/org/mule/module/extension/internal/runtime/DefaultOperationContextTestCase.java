/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import org.mule.api.MuleEvent;
import org.mule.extension.introspection.ConfigurationModel;
import org.mule.extension.introspection.ExtensionModel;
import org.mule.extension.introspection.OperationModel;
import org.mule.extension.introspection.ParameterModel;
import org.mule.module.extension.internal.config.DeclaredConfiguration;
import org.mule.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.module.extension.internal.util.ExtensionsTestUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.util.ValueHolder;

import java.util.HashMap;
import java.util.Map;

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
    private ExtensionManagerAdapter extensionManager;

    private DefaultOperationContext operationContext;

    @Before
    public void before()
    {
        Map<ParameterModel, Object> parametersMap = new HashMap<>();
        parametersMap.put(ExtensionsTestUtils.getParameter(PARAM_NAME, String.class), VALUE);
        when(resolverSetResult.asMap()).thenReturn(parametersMap);

        operationContext = new DefaultOperationContext(extensionModel, operationModel, null, resolverSetResult, event, extensionManager);
    }

    @Test
    public void onOperationSuccessful()
    {
        ValueHolder<Object> value = new ValueHolder<>();

        operationContext.onOperationSuccessful(signal -> value.set(signal.getResult()));
        operationContext.notifySuccessfulOperation(VALUE);
        assertThat(value.get(), is(VALUE));
    }

    @Test
    public void onOperationFailed()
    {
        ValueHolder<Exception> exceptionHolder = new ValueHolder<>();
        final Exception exception = new Exception();

        operationContext.onOperationFailed(signal -> exceptionHolder.set(signal.getException()));
        operationContext.notifyFailedOperation(exception);

        assertThat(exceptionHolder.get(), is(sameInstance(exception)));
    }

    @Test
    public void getImplicitConfiguration()
    {
        final Object configurationInstance = new Object();
        DeclaredConfiguration<Object> declaredConfiguration = new DeclaredConfiguration<>(CONFIG_NAME, configurationModel, configurationInstance);
        when(extensionManager.getConfiguration(extensionModel, operationContext)).thenReturn(declaredConfiguration);
        assertThat(operationContext.getConfiguration(), is(sameInstance(configurationInstance)));
    }

    @Test
    public void getSpecificConfiguration()
    {
        operationContext = new DefaultOperationContext(extensionModel, operationModel, CONFIG_NAME, resolverSetResult, event, extensionManager);

        final Object configurationInstance = new Object();
        DeclaredConfiguration<Object> declaredConfiguration = new DeclaredConfiguration<>(CONFIG_NAME, configurationModel, configurationInstance);
        when(extensionManager.getConfiguration(extensionModel, CONFIG_NAME, operationContext)).thenReturn(declaredConfiguration);
        assertThat(operationContext.getConfiguration(), is(sameInstance(configurationInstance)));
    }

    @Test
    public void getOperation()
    {
        assertThat(operationContext.getOperationModel(), is(operationModel));
    }

    @Test
    public void getParameter()
    {
        assertThat(operationContext.getParameterValue(PARAM_NAME), is(VALUE));
    }
}
