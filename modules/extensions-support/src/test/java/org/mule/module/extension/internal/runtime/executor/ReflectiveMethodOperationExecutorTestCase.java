/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.executor;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.module.extension.HealthStatus.DEAD;
import static org.mule.module.extension.HeisenbergExtension.HEISENBERG;
import org.mule.api.MuleEvent;
import org.mule.api.transformer.DataType;
import org.mule.extension.api.ExtensionManager;
import org.mule.extension.api.introspection.ConfigurationModel;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.OperationModel;
import org.mule.extension.api.introspection.ParameterModel;
import org.mule.extension.api.runtime.ConfigurationInstance;
import org.mule.extension.api.runtime.ContentMetadata;
import org.mule.extension.api.runtime.ContentType;
import org.mule.module.extension.HeisenbergExtension;
import org.mule.module.extension.HeisenbergOperations;
import org.mule.module.extension.internal.runtime.DefaultOperationContext;
import org.mule.module.extension.internal.runtime.OperationContextAdapter;
import org.mule.module.extension.internal.runtime.config.LifecycleAwareConfigurationInstance;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.module.extension.internal.util.ExtensionsTestUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
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
public class ReflectiveMethodOperationExecutorTestCase extends AbstractMuleTestCase
{

    private static final String CONFIG_NAME = "config";
    private static final DataType DATA_TYPE = DataTypeFactory.create(String.class);

    @Mock(answer = RETURNS_DEEP_STUBS)
    private MuleEvent muleEvent;

    @Mock
    private ResolverSetResult parameters;

    @Mock
    private ExtensionModel extensionModel;

    @Mock
    private ConfigurationModel configurationModel;

    @Mock
    private OperationModel operationModel;

    @Mock
    private ExtensionManager extensionManager;

    private ReflectiveMethodOperationExecutor executor;
    private ConfigurationInstance<Object> configurationInstance;
    private OperationContextAdapter operationContext;
    private HeisenbergExtension config;
    private HeisenbergOperations operations;


    @Before
    public void init() throws Exception
    {
        initHeisenberg();
        configurationInstance = new LifecycleAwareConfigurationInstance<>(CONFIG_NAME, configurationModel, config, emptyList(), Optional.empty());
        when(muleEvent.getMessage().getDataType()).thenReturn(DATA_TYPE);
        operationContext = new DefaultOperationContext(configurationInstance, parameters, operationModel, muleEvent);
        operationContext = spy(operationContext);
    }

    @Test
    public void operationWithReturnValueAndWithoutParameters() throws Exception
    {
        Method method = ClassUtils.getMethod(HeisenbergOperations.class, "sayMyName", new Class<?>[] {HeisenbergExtension.class, ContentMetadata.class});
        executor = new ReflectiveMethodOperationExecutor(method, operations, ValueReturnDelegate.INSTANCE);
        assertResult(executor.execute(operationContext), HEISENBERG);
    }

    @Test
    public void exceptionIsPropagated() throws Exception
    {
        final RuntimeException exception = new RuntimeException();
        operations = mock(HeisenbergOperations.class);
        when(operations.sayMyName(any(HeisenbergExtension.class), any(ContentMetadata.class))).thenThrow(exception);

        try
        {
            operationWithReturnValueAndWithoutParameters();
            fail("was expecting an exception");
        }
        catch (Exception e)
        {
            assertThat(e, is(sameInstance(exception)));
        }
    }

    @Test
    public void voidOperationWithoutParameters() throws Exception
    {
        Method method = ClassUtils.getMethod(HeisenbergOperations.class, "die", new Class<?>[] {HeisenbergExtension.class});
        executor = new ReflectiveMethodOperationExecutor(method, operations, VoidReturnDelegate.INSTANCE);

        assertSameInstance(executor.execute(operationContext), muleEvent);
        assertThat(config.getEndingHealth(), is(DEAD));
    }

    @Test
    public void withArgumentsAndReturnValue() throws Exception
    {
        Map<ParameterModel, Object> parametersMap = new HashMap<>();
        parametersMap.put(ExtensionsTestUtils.getParameter("index", int.class), 0);
        when(parameters.asMap()).thenReturn(parametersMap);
        init();

        Method method = ClassUtils.getMethod(HeisenbergOperations.class, "getEnemy", new Class<?>[] {HeisenbergExtension.class, int.class, ContentMetadata.class});
        executor = new ReflectiveMethodOperationExecutor(method, operations, ValueReturnDelegate.INSTANCE);

        assertResult(executor.execute(operationContext), "Hank");
    }

    @Test
    public void voidWithArguments() throws Exception
    {
        Method method = ClassUtils.getMethod(HeisenbergOperations.class, "hideMethInEvent", new Class<?>[] {MuleEvent.class, ContentType.class});
        executor = new ReflectiveMethodOperationExecutor(method, operations, VoidReturnDelegate.INSTANCE);
        assertSameInstance(executor.execute(operationContext), muleEvent);
        verify(muleEvent).setFlowVariable("secretPackage", "meth");
    }

    private void initHeisenberg()
    {
        config = new HeisenbergExtension();
        config.getPersonalInfo().setName(HEISENBERG);
        config.setEnemies(Arrays.asList("Hank"));
        operations = new HeisenbergOperations();
    }

    private void assertResult(Object value, Object expected) throws Exception
    {
        assertThat(value, is(expected));
    }

    private void assertSameInstance(Object value, Object expected) throws Exception
    {
        assertThat(value, is(sameInstance(expected)));
    }
}
