/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.internal.connection.CachedConnectionProviderWrapper;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.core.internal.connection.DefaultConnectionManager;
import org.mule.runtime.core.retry.RetryPolicyExhaustedException;
import org.mule.runtime.core.retry.policies.SimpleRetryPolicyTemplate;
import org.mule.runtime.extension.api.introspection.Interceptable;
import org.mule.runtime.extension.api.introspection.RuntimeExtensionModel;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.extension.api.introspection.exception.ExceptionEnricher;
import org.mule.runtime.extension.api.introspection.exception.ExceptionEnricherFactory;
import org.mule.runtime.extension.api.introspection.operation.RuntimeOperationModel;
import org.mule.runtime.extension.api.runtime.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.Interceptor;
import org.mule.runtime.extension.api.runtime.OperationContext;
import org.mule.runtime.extension.api.runtime.OperationExecutor;
import org.mule.runtime.extension.api.runtime.RetryRequest;
import org.mule.runtime.module.extension.internal.runtime.config.MutableConfigurationStats;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.exception.HeisenbergException;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.verification.VerificationMode;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultExecutionMediatorTestCase extends AbstractMuleContextTestCase
{

    public static final int RETRY_COUNT = 10;
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private OperationContext operationContext;

    @Mock(extraInterfaces = Interceptable.class)
    private ConfigurationInstance<Object> configurationInstance;

    @Mock
    private MutableConfigurationStats configurationStats;

    @Mock(extraInterfaces = Interceptable.class)
    private OperationExecutor operationExecutor;

    @Mock
    private OperationExecutor operationExceptionExecutor;

    @Mock
    private Interceptor configurationInterceptor1;

    @Mock
    private Interceptor configurationInterceptor2;

    @Mock
    private Interceptor operationInterceptor1;

    @Mock
    private Interceptor operationInterceptor2;

    @Mock
    private ExceptionEnricher exceptionEnricher;

    @Mock
    private RuntimeConfigurationModel configurationModel;

    @Mock
    private RuntimeExtensionModel extensionModel;

    @Mock
    private RuntimeOperationModel operationModel;

    @Mock
    private ConnectionManagerAdapter connectionManagerAdapter;

    private ConnectionException connectionException = new ConnectionException("Connection failure");

    private Exception exception = new Exception();

    private static final String DUMMY_NAME = "dummyName";
    private static final String ERROR = "Error";

    private final Object result = new Object();

    private InOrder inOrder;
    private List<Interceptor> orderedInterceptors;
    private ExecutionMediator mediator;

    @Before
    public void before() throws Exception
    {
        when(configurationInstance.getStatistics()).thenReturn(configurationStats);
        when(configurationInstance.getName()).thenReturn(DUMMY_NAME);
        when(configurationInstance.getModel()).thenReturn(configurationModel);
        when(configurationModel.getExtensionModel()).thenReturn(extensionModel);
        when(extensionModel.getName()).thenReturn(DUMMY_NAME);
        when(extensionModel.getExceptionEnricherFactory()).thenReturn(Optional.empty());
        when(operationModel.getExceptionEnricherFactory()).thenReturn(Optional.empty());
        when(operationExecutor.execute(operationContext)).thenReturn(result);
        when(operationExceptionExecutor.execute(operationContext)).thenThrow(exception);
        when(operationContext.getConfiguration()).thenReturn(configurationInstance);
        when(operationContext.getConfiguration().getModel().getExtensionModel().getName()).thenReturn(DUMMY_NAME);

        mediator = new DefaultExecutionMediator(extensionModel, operationModel, new DefaultConnectionManager(muleContext));

        final CachedConnectionProviderWrapper<Object, Object> connectionProviderWrapper = new CachedConnectionProviderWrapper<>(null, false, new SimpleRetryPolicyTemplate(10, RETRY_COUNT));
        LifecycleUtils.initialiseIfNeeded(connectionProviderWrapper, true, muleContext);
        Optional<ConnectionProvider> connectionProvider = Optional.of(connectionProviderWrapper);

        when(configurationInstance.getConnectionProvider()).thenReturn(connectionProvider);
        when(exceptionEnricher.enrichException(exception)).thenReturn(new HeisenbergException(ERROR));

        setInterceptors((Interceptable) configurationInstance, configurationInterceptor1, configurationInterceptor2);
        setInterceptors((Interceptable) operationExecutor, operationInterceptor1, operationInterceptor2);
        defineOrder(configurationInterceptor1, configurationInterceptor2, operationInterceptor1, operationInterceptor2);
    }

    @Test
    public void interceptorsInvokedOnSuccess() throws Throwable
    {
        Object result = mediator.execute(operationExecutor, operationContext);

        assertBefore();
        assertOnSuccess(times(1));
        assertOnError(never());
        assertAfter(result);
        assertResult(result);
    }

    @Test
    public void interceptorsInvokedOnError() throws Throwable
    {
        stubException();
        assertException(e -> {
            assertThat(e, is(instanceOf(ConnectionException.class)));

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
    public void decoratedException() throws Throwable
    {
        stubException();
        final Exception decoratedException = mock(Exception.class);
        when(configurationInterceptor2.onError(same(operationContext), any(RetryRequest.class), same(connectionException))).thenReturn(decoratedException);
        assertException(e -> {
            assertThat(e, is(sameInstance(decoratedException)));
            assertAfter(null);
        });
    }

    @Test
    public void exceptionOnBefore() throws Throwable
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
    public void configurationStatsOnSuccessfulOperation() throws Throwable
    {
        mediator.execute(operationExecutor, operationContext);
        assertStatistics();
    }

    @Test
    public void configurationStatsOnFailedOperation() throws Throwable
    {
        stubException();
        assertException(e -> assertStatistics());
    }

    @Test
    public void configurationStatsOnFailedBeforeInterceptor() throws Throwable
    {
        stubExceptionOnBeforeInterceptor();
        assertException(e -> assertStatistics());
    }

    @Test
    public void enrichThrownException() throws Throwable
    {
        expectedException.expect(HeisenbergException.class);
        expectedException.expectMessage(ERROR);
        ExceptionEnricherFactory exceptionEnricherFactory = mock(ExceptionEnricherFactory.class);
        when(exceptionEnricherFactory.createEnricher()).thenReturn(exceptionEnricher);
        when(operationModel.getExceptionEnricherFactory()).thenReturn(Optional.of(exceptionEnricherFactory));
        new DefaultExecutionMediator(extensionModel, operationModel, new DefaultConnectionManager(muleContext)).execute(operationExceptionExecutor, operationContext);
    }

    public class DummyConnectionInterceptor implements Interceptor
    {

        @Override
        public Throwable onError(OperationContext operationContext, RetryRequest retryRequest, Throwable exception)
        {
            retryRequest.request();
            return exception;
        }
    }

    @Test
    public void retry() throws Throwable
    {
        stubException();
        Interceptor interceptor = mock(Interceptor.class);
        setInterceptors((Interceptable) configurationInstance, interceptor, new DummyConnectionInterceptor());
        setInterceptors((Interceptable) operationExecutor, new DummyConnectionInterceptor());

        defineOrder(interceptor);
        assertException(exception -> {
            assertThat(exception, instanceOf(RetryPolicyExhaustedException.class));
            try
            {
                verify(interceptor, times(RETRY_COUNT + 1)).before(operationContext);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
            verify(interceptor, times(RETRY_COUNT + 1)).onError(same(operationContext), any(RetryRequest.class), anyVararg());
            verify(interceptor, times(RETRY_COUNT + 1)).after(operationContext, null);
        });
    }

    private void assertException(Consumer<Throwable> assertion) throws Throwable
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
        doThrow(exception).when(operationInterceptor2).before(operationContext);
    }

    private void assertStatistics()
    {
        verify(configurationStats).addInflightOperation();
        verify(configurationStats).discountInflightOperation();
    }

    private void assertBefore() throws Exception
    {
        verifyInOrder(interceptor -> {
            try
            {
                interceptor.before(operationContext);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }

        });
    }

    private void assertOnSuccess(VerificationMode verificationMode)
    {
        verifyInOrder(interceptor -> interceptor.onSuccess(operationContext, result), verificationMode);
    }

    private void assertOnError(VerificationMode verificationMode)
    {
        verifyInOrder(interceptor -> interceptor.onError(same(operationContext), any(RetryRequest.class), same(connectionException)), verificationMode);
    }

    private void assertAfter(Object expected)
    {
        verifyInOrder(interceptor -> interceptor.after(operationContext, expected));
    }

    private void assertResult(Object result)
    {
        assertThat(result, is(sameInstance(this.result)));
    }

    private void stubException() throws Exception
    {
        when(operationExecutor.execute(operationContext)).thenThrow(connectionException);
    }

    private void setInterceptors(Interceptable interceptable, Interceptor... interceptors)
    {
        when(interceptable.getInterceptors()).thenReturn(asList(interceptors));
    }

    private void defineOrder(Interceptor... interceptors)
    {
        inOrder = inOrder(interceptors);
        orderedInterceptors = ImmutableList.copyOf(interceptors);
    }

    private void verifyInOrder(Consumer<Interceptor> consumer)
    {
        verifyInOrder(consumer, times(1));
    }

    private void verifyInOrder(Consumer<Interceptor> consumer, VerificationMode verificationMode)
    {
        for (Interceptor interceptor : orderedInterceptors)
        {
            consumer.accept(inOrder.verify(interceptor, verificationMode));
        }
    }
}
