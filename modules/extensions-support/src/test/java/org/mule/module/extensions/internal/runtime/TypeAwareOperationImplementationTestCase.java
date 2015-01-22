/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.runtime;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.module.extensions.HealthStatus.DEAD;
import static org.mule.module.extensions.HeisenbergExtension.HEISENBERG;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.extensions.annotations.WithConfig;
import org.mule.extensions.introspection.OperationContext;
import org.mule.extensions.introspection.Parameter;
import org.mule.module.extensions.HeisenbergExtension;
import org.mule.module.extensions.HeisenbergOperations;
import org.mule.module.extensions.internal.runtime.resolver.ResolverSetResult;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class TypeAwareOperationImplementationTestCase extends AbstractMuleTestCase
{

    @Mock(answer = RETURNS_DEEP_STUBS)
    private MuleEvent muleEvent;

    @Mock
    private ResolverSetResult parameters;

    private Map<Parameter, Object> parameterValues = new HashMap<>();

    private TypeAwareOperationImplementation implementation;
    private HeisenbergExtension config;
    private OperationContext operationContext;


    @Before
    public void before()
    {
        initHeisenberg();
        operationContext = new DefaultOperationContext(config, parameters, muleEvent, null);
        when(operationContext.getParametersValues()).thenReturn(parameterValues);
    }

    @Test
    public void operationWithReturnValueAndWithoutParameters() throws Exception
    {
        Method method = ClassUtils.getMethod(HeisenbergOperations.class, "sayMyName", new Class<?>[] {});
        implementation = new TypeAwareOperationImplementation(HeisenbergOperations.class, method);
        assertResult(implementation.execute(operationContext), HEISENBERG);
    }

    @Test
    public void voidOperationWithoutParameters() throws Exception
    {
        Method method = ClassUtils.getMethod(HeisenbergOperations.class, "die", new Class<?>[] {});
        implementation = new TypeAwareOperationImplementation(HeisenbergOperations.class, method);
        assertSameInstance(implementation.execute(operationContext), muleEvent);
        assertThat(config.getFinalHealth(), is(DEAD));
    }

    @Test
    public void withArgumentsAndReturnValue() throws Exception
    {
        Method method = ClassUtils.getMethod(HeisenbergOperations.class, "getEnemy", new Class<?>[] {int.class});
        implementation = new TypeAwareOperationImplementation(HeisenbergOperations.class, method);
        parameterValues.put(mock(Parameter.class), 0);
        assertResult(implementation.execute(operationContext), "Hank");
    }

    @Test
    public void voidWithArguments() throws Exception
    {
        HeisenbergOperations.eventHolder.set(muleEvent);
        Method method = ClassUtils.getMethod(HeisenbergOperations.class, "hideMethInEvent", new Class<?>[] {});
        implementation = new TypeAwareOperationImplementation(HeisenbergOperations.class, method);
        assertSameInstance(implementation.execute(operationContext), muleEvent);
        verify(muleEvent).setFlowVariable("secretPackage", "meth");
    }

    @Test(expected = IllegalArgumentException.class)
    public void operationWithTwoConfigs() throws Exception
    {
        Method method = ClassUtils.getMethod(HeisenbergOperations.class, "hideMethInEvent", new Class<?>[] {});
        new TypeAwareOperationImplementation(TwoConfigs.class, method);
    }

    @Test(expected = IllegalArgumentException.class)
    public void operationWithLifecycle() throws Exception
    {
        Method method = ClassUtils.getMethod(HeisenbergOperations.class, "hideMethInEvent", new Class<?>[] {});
        new TypeAwareOperationImplementation(WithLifecycle.class, method);
    }

    private void initHeisenberg()
    {
        config = new HeisenbergExtension();
        config.getPersonalInfo().setMyName(HEISENBERG);
        config.setEnemies(Arrays.asList("Hank"));
        HeisenbergOperations.eventHolder.set(muleEvent);
    }

    private void assertResult(Future<Object> result, Object expected) throws Exception
    {
        Object value = result.get();
        assertThat(value, is(expected));
    }

    private void assertSameInstance(Future<Object> result, Object expected) throws Exception
    {
        Object value = result.get();
        assertThat(value, is(sameInstance(expected)));
    }

    public static class TwoConfigs extends HeisenbergOperations
    {

        @WithConfig
        private HeisenbergExtension config;

        public TwoConfigs()
        {
        }
    }

    public static class WithLifecycle extends HeisenbergOperations implements Lifecycle
    {

        @Override
        public void dispose()
        {

        }

        @Override
        public void initialise() throws InitialisationException
        {

        }

        @Override
        public void start() throws MuleException
        {

        }

        @Override
        public void stop() throws MuleException
        {

        }
    }
}
