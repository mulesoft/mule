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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.module.extension.HealthStatus.DEAD;
import static org.mule.module.extension.HeisenbergExtension.HEISENBERG;
import org.mule.api.MuleEvent;
import org.mule.extension.introspection.Operation;
import org.mule.extension.runtime.OperationContext;
import org.mule.module.extension.HeisenbergExtension;
import org.mule.module.extension.HeisenbergOperations;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ReflectiveMethodOperationExecutorTestCase extends AbstractMuleTestCase
{

    @Mock(answer = RETURNS_DEEP_STUBS)
    private MuleEvent muleEvent;

    @Mock
    private ResolverSetResult parameters;

    @Mock
    private Operation operation;

    private ReflectiveMethodOperationExecutor executor;
    private HeisenbergExtension config;
    private OperationContext operationContext;
    private HeisenbergOperations operations;


    @Before
    public void before()
    {
        initHeisenberg();
        operationContext = new DefaultOperationContext(operation, parameters, muleEvent);
    }

    @Test
    public void operationWithReturnValueAndWithoutParameters() throws Exception
    {
        Method method = ClassUtils.getMethod(HeisenbergOperations.class, "sayMyName", new Class<?>[] {});
        executor = new ReflectiveMethodOperationExecutor(method, operations, ValueReturnDelegate.INSTANCE);
        assertResult(executor.execute(operationContext), HEISENBERG);
    }

    @Test
    public void voidOperationWithoutParameters() throws Exception
    {
        Method method = ClassUtils.getMethod(HeisenbergOperations.class, "die", new Class<?>[] {});
        executor = new ReflectiveMethodOperationExecutor(method, operations, VoidReturnDelegate.INSTANCE);

        assertSameInstance(executor.execute(operationContext), muleEvent);
        assertThat(config.getEndingHealth(), is(DEAD));
    }

    @Test
    public void withArgumentsAndReturnValue() throws Exception
    {
        operationContext = mock(OperationContext.class);
        when(operationContext.getParameterValue("index")).thenReturn(0);

        Method method = ClassUtils.getMethod(HeisenbergOperations.class, "getEnemy", new Class<?>[] {int.class});
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
        operations = new HeisenbergOperations(config);
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
