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
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.module.extension.HealthStatus.DEAD;
import static org.mule.module.extension.HeisenbergExtension.HEISENBERG;
import org.mule.api.MuleEvent;
import org.mule.extension.introspection.Extension;
import org.mule.extension.introspection.Operation;
import org.mule.extension.introspection.Parameter;
import org.mule.extension.runtime.OperationContext;
import org.mule.module.extension.HeisenbergExtension;
import org.mule.module.extension.HeisenbergOperations;
import org.mule.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.module.extension.internal.util.ExtensionsTestUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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

    @Mock(answer = RETURNS_DEEP_STUBS)
    private MuleEvent muleEvent;

    @Mock
    private ResolverSetResult parameters;

    @Mock
    private Extension extension;

    @Mock
    private Operation operation;

    @Mock
    private ExtensionManagerAdapter extensionManager;

    private ReflectiveMethodOperationExecutor executor;
    private OperationContext operationContext;
    private HeisenbergExtension config;
    private HeisenbergOperations operations;


    @Before
    public void init()
    {
        initHeisenberg();
        operationContext = new DefaultOperationContext(extension, operation, CONFIG_NAME, parameters, muleEvent, extensionManager);
        when(extensionManager.getConfigurationInstance(extension, CONFIG_NAME, operationContext)).thenReturn(config);
    }

    @Test
    public void operationWithReturnValueAndWithoutParameters() throws Exception
    {
        Method method = ClassUtils.getMethod(HeisenbergOperations.class, "sayMyName", new Class<?>[] {HeisenbergExtension.class});
        executor = new ReflectiveMethodOperationExecutor(method, operations, ValueReturnDelegate.INSTANCE);
        assertResult(executor.execute(operationContext), HEISENBERG);
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
        Map<Parameter, Object> parametersMap = new HashMap<>();
        parametersMap.put(ExtensionsTestUtils.getParameter("index", int.class), 0);
        when(parameters.asMap()).thenReturn(parametersMap);
        init();

        Method method = ClassUtils.getMethod(HeisenbergOperations.class, "getEnemy", new Class<?>[] {HeisenbergExtension.class, int.class});
        executor = new ReflectiveMethodOperationExecutor(method, operations, ValueReturnDelegate.INSTANCE);

        assertResult(executor.execute(operationContext), "Hank");
    }

    @Test
    public void voidWithArguments() throws Exception
    {
        Method method = ClassUtils.getMethod(HeisenbergOperations.class, "hideMethInEvent", new Class<?>[] {MuleEvent.class});
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
