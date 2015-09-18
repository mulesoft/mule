/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.extension.api.introspection.Interceptable;
import org.mule.extension.api.runtime.ConfigurationInstance;
import org.mule.extension.api.runtime.Interceptor;
import org.mule.extension.api.runtime.OperationContext;
import org.mule.extension.api.runtime.OperationExecutor;
import org.mule.extension.api.runtime.RetryRequest;
import org.mule.module.extension.internal.runtime.config.MutableConfigurationStats;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.verification.VerificationMode;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultExecutionMediatorTestCase extends AbstractMuleTestCase
{

    @Mock
    private OperationContext operationContext;

    @Mock(extraInterfaces = Interceptable.class)
    private ConfigurationInstance<Object> configurationInstance;

    @Mock
    private MutableConfigurationStats configurationStats;

    @Mock
    private OperationExecutor operationExecutor;

    @Mock
    private Interceptor interceptor1;

    @Mock
    private Interceptor interceptor2;

    @Mock
    private Exception exception;

    private InOrder inOrder;

    private ExecutionMediator mediator = new DefaultExecutionMediator();
    private final Object result = new Object();

    @Before
    public void before() throws Exception
    {
        when(configurationInstance.getStatistics()).thenReturn(configurationStats);
        when(operationExecutor.execute(operationContext)).thenReturn(result);
        when(operationContext.getConfiguration()).thenReturn(configurationInstance);

        setInterceptors(interceptor1, interceptor2);
    }

    @Test
    public void interceptorsInvokedOnSuccess() throws Exception
    {
        Object result = mediator.execute(operationExecutor, operationContext);

        assertBefore();
        assertOnSuccess(times(1));
        assertOnError(never());
        assertAfter(result);
        assertResult(result);
    }

    @Test
    public void interceptorsInvokedOnError() throws Exception
    {
        stubException();
        assertException(e -> {
            assertThat(e, is(sameInstance(exception)));

            try
            {
                assertBefore();
            }
            catch (Exception e2)
            {
                throw new RuntimeException(e2);
            }
            assertOnSuccess(never());
            assertOnError(times(1));
            assertAfter(null);
        });
    }

    @Test
    public void decoratedException() throws Exception
    {
        stubException();
        final Exception decoratedException = mock(Exception.class);
        when(interceptor2.onError(same(operationContext), any(RetryRequest.class), same(exception))).thenReturn(decoratedException);

        assertException(e -> {
            assertThat(e, is(sameInstance(decoratedException)));
            assertAfter(null);
        });
    }

    @Test
    public void exceptionOnBefore() throws Exception
    {
        stubExceptionOnBeforeInterceptor();
        assertException(e -> {
            try
            {
                assertThat(e, is(sameInstance(exception)));
                assertBefore();
                assertOnError(never());
                verify(operationExecutor, never()).execute(operationContext);
            }
            catch (Exception e2)
            {
                throw new RuntimeException(e2);
            }
        });
    }

    @Test
    public void configurationStatsOnSuccessfulOperation() throws Exception
    {
        mediator.execute(operationExecutor, operationContext);
        assertStatistics();
    }

    @Test
    public void configurationStatsOnFailedOperation() throws Exception
    {
        stubException();
        assertException(e -> assertStatistics());
    }

    @Test
    public void configurationStatsOnFailedBeforeInterceptor() throws Exception
    {
        stubExceptionOnBeforeInterceptor();
        assertException(e -> assertStatistics());
    }

    @Test
    public void retry() throws Exception
    {
        stubException();
        Interceptor interceptor = mock(Interceptor.class);
        setInterceptors(interceptor);
        when(interceptor.onError(same(operationContext), any(RetryRequest.class), same(exception)))
                .then(invocation -> {
                    RetryRequest retryRequest = (RetryRequest) invocation.getArguments()[1];
                    retryRequest.request();

                    return invocation.getArguments()[2];
                });

        assertException(exception -> {
            assertThat(exception, is(sameInstance(this.exception)));
            try
            {
                verify(interceptor, times(2)).before(operationContext);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
            verify(interceptor, times(2)).onError(same(operationContext), any(RetryRequest.class), same(exception));
            verify(interceptor, times(2)).after(operationContext, null);
        });
    }

    private void assertException(Consumer<Exception> assertion)
    {
        try
        {
            mediator.execute(operationExecutor, operationContext);
            fail("was expecting a exception");
        }
        catch (Exception e)
        {
            assertion.accept(e);
        }
    }

    private void stubExceptionOnBeforeInterceptor() throws Exception
    {
        doThrow(exception).when(interceptor2).before(operationContext);
    }

    private void assertStatistics()
    {
        verify(configurationStats).addInflightOperation();
        verify(configurationStats).discountInflightOperation();
    }

    private void assertBefore() throws Exception
    {
        inOrder.verify(interceptor1).before(operationContext);
        inOrder.verify(interceptor2).before(operationContext);
    }

    private void assertOnSuccess(VerificationMode verificationMode)
    {
        inOrder.verify(interceptor1, verificationMode).onSuccess(operationContext, result);
        inOrder.verify(interceptor2, verificationMode).onSuccess(operationContext, result);
    }

    private void assertOnError(VerificationMode verificationMode)
    {
        inOrder.verify(interceptor1, verificationMode).onError(same(operationContext), any(RetryRequest.class), same(exception));
        inOrder.verify(interceptor2, verificationMode).onError(same(operationContext), any(RetryRequest.class), same(exception));
    }


    private void assertAfter(Object expected)
    {
        inOrder.verify(interceptor1).after(operationContext, expected);
        inOrder.verify(interceptor2).after(operationContext, expected);
    }

    private void assertResult(Object result)
    {
        assertThat(result, is(sameInstance(this.result)));
    }

    private void stubException() throws Exception
    {
        when(operationExecutor.execute(operationContext)).thenThrow(exception);
    }

    private void setInterceptors(Interceptor... interceptors)
    {
        Interceptable interceptable = (Interceptable) configurationInstance;
        when(interceptable.getInterceptors()).thenReturn(asList(interceptors));
        inOrder = inOrder(interceptors);
    }

}
