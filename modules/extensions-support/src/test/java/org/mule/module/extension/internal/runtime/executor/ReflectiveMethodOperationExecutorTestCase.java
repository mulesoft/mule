/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.executor;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mule.module.extension.HeisenbergExtension.HEISENBERG;
import static org.mule.module.extension.model.HealthStatus.DEAD;
import org.mule.api.MuleEvent;
import org.mule.api.metadata.DataType;
import org.mule.api.temporary.MuleMessage;
import org.mule.extension.annotation.api.Operation;
import org.mule.extension.api.ExtensionManager;
import org.mule.extension.api.introspection.ConfigurationModel;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.OperationModel;
import org.mule.extension.api.introspection.ParameterModel;
import org.mule.extension.api.runtime.ConfigurationInstance;
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
    private PrimitiveTypesTestOperations primitiveTypesTestOperations = new PrimitiveTypesTestOperations();


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
        Method method = ClassUtils.getMethod(HeisenbergOperations.class, "sayMyName", new Class<?>[] {HeisenbergExtension.class});
        executor = new ReflectiveMethodOperationExecutor(method, operations);
        assertResult(executor.execute(operationContext), HEISENBERG);
    }

    @Test
    public void exceptionIsPropagated() throws Exception
    {
        final RuntimeException exception = new RuntimeException();
        operations = mock(HeisenbergOperations.class);
        when(operations.sayMyName(any(HeisenbergExtension.class))).thenThrow(exception);

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
        executor = new ReflectiveMethodOperationExecutor(method, operations);

        assertThat(executor.execute(operationContext), is(nullValue()));
        assertThat(config.getEndingHealth(), is(DEAD));
    }

    @Test
    public void withArgumentsAndReturnValue() throws Exception
    {
        Map<ParameterModel, Object> parametersMap = new HashMap<>();
        parametersMap.put(ExtensionsTestUtils.getParameter("index", int.class), 0);
        when(parameters.asMap()).thenReturn(parametersMap);
        init();

        Method method = ClassUtils.getMethod(HeisenbergOperations.class, "getEnemy", new Class<?>[] {HeisenbergExtension.class, int.class});
        executor = new ReflectiveMethodOperationExecutor(method, operations);

        assertResult(((MuleMessage)executor.execute(operationContext)).getPayload(), "Hank");
    }

    @Test
    public void voidWithArguments() throws Exception
    {
        Method method = ClassUtils.getMethod(HeisenbergOperations.class, "die", new Class<?>[] {HeisenbergExtension.class});
        executor = new ReflectiveMethodOperationExecutor(method, operations);
        assertThat(executor.execute(operationContext), is(nullValue()));
    }

    @Test
    public void withPrimitiveTypeArgumentsWithoutValue() throws Exception
    {
        Object[][] primitiveOperations = {
                {"charOperation", char.class},
                {"byteOperation", byte.class},
                {"shortOperation", short.class},
                {"intOperation", int.class},
                {"longOperation", long.class},
                {"floatOperation", float.class},
                {"doubleOperation", double.class},
                {"booleanOperation", boolean.class}
        };
        for (Object[] primitiveOperation : primitiveOperations)
        {
            Method method = ClassUtils.getMethod(PrimitiveTypesTestOperations.class, (String) primitiveOperation[0], new Class<?>[] {(Class<?>) primitiveOperation[1]});
            executor = new ReflectiveMethodOperationExecutor(method, primitiveTypesTestOperations);
            executor.execute(operationContext);
        }
    }

    @Test
    public void withAllPrimitiveTypeArgumentsWithoutValue() throws Exception
    {
        Class<?>[] parameterTypes = {char.class, byte.class, short.class, int.class, long.class, float.class, double.class, boolean.class};
        Method method = ClassUtils.getMethod(PrimitiveTypesTestOperations.class, "allCombined", parameterTypes);
        executor = new ReflectiveMethodOperationExecutor(method, primitiveTypesTestOperations);
        executor.execute(operationContext);
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

    public static class PrimitiveTypesTestOperations
    {

        private char charValue;
        private byte byteValue;
        private short shortValue;
        private int intValue;
        private long longValue;
        private float floatValue;
        private double doubleValue;
        private boolean booleanValue;

        @Operation
        public void charOperation(@org.mule.extension.annotation.api.param.Optional char value)
        {
            assertThat(value, is(charValue));
        }

        @Operation
        public void byteOperation(@org.mule.extension.annotation.api.param.Optional byte value)
        {
            assertThat(value, is(byteValue));
        }

        @Operation
        public void shortOperation(@org.mule.extension.annotation.api.param.Optional short value)
        {
            assertThat(value, is(shortValue));
        }

        @Operation
        public void intOperation(@org.mule.extension.annotation.api.param.Optional int value)
        {
            assertThat(value, is(intValue));
        }

        @Operation
        public void longOperation(@org.mule.extension.annotation.api.param.Optional long value)
        {
            assertThat(value, is(longValue));
        }

        @Operation
        public void floatOperation(@org.mule.extension.annotation.api.param.Optional float value)
        {
            assertThat(value, is(floatValue));
        }

        @Operation
        public void doubleOperation(@org.mule.extension.annotation.api.param.Optional double value)
        {
            assertThat(value, is(doubleValue));
        }

        @Operation
        public void booleanOperation(@org.mule.extension.annotation.api.param.Optional boolean value)
        {
            assertThat(value, is(booleanValue));
        }

        @Operation
        public void allCombined(@org.mule.extension.annotation.api.param.Optional char charValue,
                                @org.mule.extension.annotation.api.param.Optional byte byteValue,
                                @org.mule.extension.annotation.api.param.Optional short shortValue,
                                @org.mule.extension.annotation.api.param.Optional int intValue,
                                @org.mule.extension.annotation.api.param.Optional long longValue,
                                @org.mule.extension.annotation.api.param.Optional float floatValue,
                                @org.mule.extension.annotation.api.param.Optional double doubleValue,
                                @org.mule.extension.annotation.api.param.Optional boolean booleanValue)
        {
            assertThat(charValue, is(this.charValue));
            assertThat(byteValue, is(this.byteValue));
            assertThat(shortValue, is(this.shortValue));
            assertThat(intValue, is(this.intValue));
            assertThat(longValue, is(this.longValue));
            assertThat(floatValue, is(this.floatValue));
            assertThat(doubleValue, is(this.doubleValue));
            assertThat(booleanValue, is(this.booleanValue));
        }
    }
}
