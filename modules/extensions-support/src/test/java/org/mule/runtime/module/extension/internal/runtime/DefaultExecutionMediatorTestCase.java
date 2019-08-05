/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyVararg;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.junit.MockitoJUnit.rule;
import static org.mule.functional.junit4.matchers.ThrowableRootCauseMatcher.hasRootCause;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.internal.util.rx.ImmediateScheduler.IMMEDIATE_SCHEDULER;
import static org.mule.test.heisenberg.extension.HeisenbergErrors.HEALTH;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockExceptionEnricher;
import static reactor.core.Exceptions.unwrap;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.TypedException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.error.ImmutableErrorModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.retry.policy.NoRetryPolicyTemplate;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.core.internal.connection.DefaultConnectionManager;
import org.mule.runtime.core.internal.connection.ReconnectableConnectionProviderWrapper;
import org.mule.runtime.core.internal.message.ErrorTypeBuilder;
import org.mule.runtime.core.internal.retry.ReconnectionConfig;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.property.ClassLoaderModelProperty;
import org.mule.runtime.extension.api.runtime.Interceptable;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.exception.ExceptionHandler;
import org.mule.runtime.extension.api.runtime.operation.ComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.config.MutableConfigurationStats;
import org.mule.runtime.module.extension.internal.runtime.operation.DefaultExecutionMediator;
import org.mule.runtime.module.extension.internal.runtime.operation.DefaultExecutionMediator.ValueTransformer;
import org.mule.runtime.module.extension.internal.runtime.operation.ExecutionMediator;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.exception.HeisenbergException;
import org.mule.test.heisenberg.extension.exception.NullExceptionEnricher;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;
import org.mockito.verification.VerificationMode;
import reactor.core.publisher.Mono;

@SmallTest
@RunWith(Parameterized.class)
public class DefaultExecutionMediatorTestCase extends AbstractMuleContextTestCase {

  public static final int RETRY_COUNT = 10;
  private static final String DUMMY_NAME = "dummyName";
  private static final String ERROR = "Error";
  private final Object result = new Object();

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[] {"With simple retry", new SimpleRetryPolicyTemplate(10, RETRY_COUNT)},
                         new Object[] {"With no retry", new NoRetryPolicyTemplate()});
  }

  public DefaultExecutionMediatorTestCase(String name, RetryPolicyTemplate retryPolicy) {
    this.name = name;
    this.retryPolicy = retryPolicy;
  }

  @Rule
  public MockitoRule mockito = rule();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExecutionContextAdapter operationContext;

  @Mock(extraInterfaces = Interceptable.class, lenient = true)
  private ConfigurationInstance configurationInstance;

  @Mock
  private MutableConfigurationStats configurationStats;

  @Mock(extraInterfaces = Interceptable.class, lenient = true)
  private ComponentExecutor operationExecutor;

  @Mock
  private ComponentExecutor operationExceptionExecutor;

  @Mock
  private Interceptor configurationInterceptor1;

  @Mock
  private Interceptor configurationInterceptor2;

  @Mock
  private Interceptor operationInterceptor1;

  @Mock
  private Interceptor operationInterceptor2;

  @Mock
  private ExceptionHandler exceptionEnricher;

  @Mock
  private ConfigurationModel configurationModel;

  @Mock(lenient = true)
  private ExtensionModel extensionModel;

  @Mock
  private OperationModel operationModel;

  @Mock
  private ConnectionManagerAdapter connectionManagerAdapter;

  private String name;
  private final RetryPolicyTemplate retryPolicy;
  private final ConnectionException connectionException = new ConnectionException("Connection failure");
  private final Exception exception = new Exception();
  private InOrder inOrder;
  private List<Interceptor> orderedInterceptors;
  private ExecutionMediator mediator;

  @Before
  public void before() throws Exception {
    when(configurationInstance.getStatistics()).thenReturn(configurationStats);
    when(configurationInstance.getName()).thenReturn(DUMMY_NAME);
    when(configurationInstance.getModel()).thenReturn(configurationModel);
    when(extensionModel.getName()).thenReturn(DUMMY_NAME);
    when(extensionModel.getModelProperty(ClassLoaderModelProperty.class)).thenReturn(empty());
    mockExceptionEnricher(extensionModel, null);
    mockExceptionEnricher(operationModel, null);
    when(operationExecutor.execute(operationContext)).thenReturn(just(result));
    when(operationExceptionExecutor.execute(operationContext)).thenReturn(error(exception));
    when(operationContext.getConfiguration()).thenReturn(Optional.of(configurationInstance));
    when(operationContext.getExtensionModel()).thenReturn(extensionModel);
    when(operationContext.getTransactionConfig()).thenReturn(empty());
    when(operationContext.getRetryPolicyTemplate()).thenReturn(ofNullable(retryPolicy));
    when(operationContext.getCurrentScheduler()).thenReturn(IMMEDIATE_SCHEDULER);

    when(extensionModel.getXmlDslModel()).thenReturn(XmlDslModel.builder().setPrefix("test-extension").build());
    mediator = new DefaultExecutionMediator(extensionModel, operationModel, new DefaultConnectionManager(muleContext),
                                            muleContext.getErrorTypeRepository());

    final ReconnectableConnectionProviderWrapper<Object> connectionProviderWrapper =
        new ReconnectableConnectionProviderWrapper<>(null,
                                                     new ReconnectionConfig(true,
                                                                            new SimpleRetryPolicyTemplate(10, RETRY_COUNT)));
    initialiseIfNeeded(connectionProviderWrapper, true, muleContext);
    Optional<ConnectionProvider> connectionProvider = Optional.of(connectionProviderWrapper);

    when(configurationInstance.getConnectionProvider()).thenReturn(connectionProvider);
    when(exceptionEnricher.enrichException(any())).thenAnswer(inv -> {
      final Throwable toEnrich = inv.getArgument(0);
      if (toEnrich == exception || toEnrich.getCause() == exception) {
        return new HeisenbergException(ERROR, toEnrich);
      } else {
        return toEnrich;
      }
    });

    setInterceptors((Interceptable) configurationInstance, configurationInterceptor1, configurationInterceptor2);
    setInterceptors((Interceptable) operationExecutor, operationInterceptor1, operationInterceptor2);
    defineOrder(configurationInterceptor1, configurationInterceptor2, operationInterceptor1, operationInterceptor2);
  }

  @Test
  public void interceptorsInvokedOnSuccess() throws Throwable {
    Object result = execute().block();

    assertBefore();
    assertOnSuccess(times(1));
    assertOnError(never());
    assertAfter(result);
    assertResult(result);
  }

  @Test
  public void interceptorsInvokedOnError() throws Throwable {
    stubException();
    assertException(e -> {
      assertThat(e, is(instanceOf(ConnectionException.class)));

      try {
        assertBefore();
      } catch (Exception e2) {
        throw new RuntimeException(e2);
      }
      assertOnSuccess(never());
      assertOnError(times(1));
      assertAfter(null);
    });
  }

  @Test
  public void decoratedException() throws Throwable {
    stubException();
    final Exception decoratedException = mock(Exception.class);
    when(configurationInterceptor2.onError(same(operationContext), same(connectionException)))
        .thenReturn(decoratedException);
    assertException(e -> {
      assertThat(e, is(sameInstance(decoratedException)));
      assertAfter(null);
    });
  }

  @Test
  public void exceptionOnBefore() throws Throwable {
    stubExceptionOnBeforeInterceptor();
    assertException(e -> {
      try {
        assertThat(e, is(sameInstance(exception)));
        assertBefore();
        assertOnError(never());
        verify(operationExecutor, never()).execute(operationContext);
      } catch (Exception e2) {
        throw new RuntimeException(e2);
      }
    });
  }

  @Test
  public void configurationStatsOnSuccessfulOperation() throws Throwable {
    execute().block();
    assertStatistics();
  }

  @Test
  public void configurationStatsOnFailedOperation() throws Throwable {
    stubException();
    assertException(e -> assertStatistics());
  }

  @Test
  public void configurationStatsOnFailedBeforeInterceptor() throws Throwable {
    stubExceptionOnBeforeInterceptor();
    assertException(e -> assertStatistics());
  }

  @Test
  public void enrichThrownException() throws Throwable {
    expectedException.expect(hasRootCause(sameInstance(exception)));
    expectedException.expectCause(instanceOf(HeisenbergException.class));
    expectedException.expectMessage(ERROR);
    mockExceptionEnricher(operationModel, () -> exceptionEnricher);
    when(operationExecutor.execute(any())).thenReturn(Mono.error(exception));
    Mono.from(new DefaultExecutionMediator(extensionModel, operationModel, new DefaultConnectionManager(muleContext),
                                           muleContext.getErrorTypeRepository())
                                               .execute(operationExceptionExecutor, operationContext))
        .block();
  }

  @Test
  public void notEnrichThrownException() throws Throwable {
    expectedException.expectCause(sameInstance(exception));
    mockExceptionEnricher(operationModel, () -> new NullExceptionEnricher());
    when(operationExecutor.execute(any())).thenReturn(Mono.error(exception));
    Mono.from(new DefaultExecutionMediator(extensionModel, operationModel, new DefaultConnectionManager(muleContext),
                                           muleContext.getErrorTypeRepository())
                                               .execute(operationExceptionExecutor, operationContext))
        .block();
  }

  @Test
  public void enrichThrownExceptionInValueTransformer() throws Throwable {
    final Exception exceptionToThrow = new RuntimeException(ERROR, exception);
    expectedException.expect(hasRootCause(sameInstance(exception)));
    expectedException.expectCause(instanceOf(HeisenbergException.class));
    expectedException.expectMessage(ERROR);
    mockExceptionEnricher(operationModel, () -> exceptionEnricher);
    final ValueTransformer failingTransformer = mock(ValueTransformer.class);
    when(failingTransformer.apply(any(), any())).thenThrow(exceptionToThrow);

    Mono.from(new DefaultExecutionMediator(extensionModel, operationModel, new DefaultConnectionManager(muleContext),
                                           muleContext.getErrorTypeRepository(), failingTransformer)
                                               .execute(operationExecutor, operationContext))
        .block();
  }

  @Test
  public void enrichThrownModuleExceptionInValueTransformer() throws Throwable {
    final ModuleException moduleExceptionToThrow = new ModuleException(ERROR, HEALTH, exception);
    expectedException.expectCause(instanceOf(HeisenbergException.class));
    expectedException.expectMessage(ERROR);
    mockExceptionEnricher(operationModel, () -> exceptionEnricher);
    final ValueTransformer failingTransformer = mock(ValueTransformer.class);
    when(failingTransformer.apply(any(), any())).thenThrow(moduleExceptionToThrow);

    ErrorTypeRepository errorTypeRepository = mockErrorModel();

    Mono.from(new DefaultExecutionMediator(extensionModel, operationModel, new DefaultConnectionManager(muleContext),
                                           errorTypeRepository, failingTransformer)
                                               .execute(operationExecutor, operationContext))
        .block();
  }

  @Test
  public void notEnrichThrownExceptionInValueTransformer() throws Throwable {
    final Exception exceptionToThrow = new RuntimeException(ERROR, exception);
    expectedException.expectCause(sameInstance(exception));
    expectedException.expectMessage(ERROR);
    mockExceptionEnricher(operationModel, () -> new NullExceptionEnricher());
    final ValueTransformer failingTransformer = mock(ValueTransformer.class);
    when(failingTransformer.apply(any(), any())).thenThrow(exceptionToThrow);

    Mono.from(new DefaultExecutionMediator(extensionModel, operationModel, new DefaultConnectionManager(muleContext),
                                           muleContext.getErrorTypeRepository(), failingTransformer)
                                               .execute(operationExecutor, operationContext))
        .block();
  }

  @Test
  public void notEnrichThrownModuleExceptionInValueTransformer() throws Throwable {
    final ModuleException moduleExceptionToThrow = new ModuleException(ERROR, HEALTH, exception);
    expectedException.expect(instanceOf(TypedException.class));
    expectedException.expectCause(sameInstance(exception));
    mockExceptionEnricher(operationModel, () -> new NullExceptionEnricher());
    final ValueTransformer failingTransformer = mock(ValueTransformer.class);
    when(failingTransformer.apply(any(), any())).thenThrow(moduleExceptionToThrow);

    ErrorTypeRepository errorTypeRepository = mockErrorModel();

    Mono.from(new DefaultExecutionMediator(extensionModel, operationModel, new DefaultConnectionManager(muleContext),
                                           errorTypeRepository, failingTransformer)
                                               .execute(operationExecutor, operationContext))
        .block();
  }

  private ErrorTypeRepository mockErrorModel() {
    final ErrorType parentErrorType = mock(ErrorType.class);
    ErrorTypeRepository errorTypeRepository = mock(ErrorTypeRepository.class);
    when(errorTypeRepository.lookupErrorType(any())).thenReturn(Optional.of(ErrorTypeBuilder.builder()
        .namespace("testNs")
        .identifier("test")
        .parentErrorType(parentErrorType)
        .build()));

    when(operationModel.getErrorModels()).thenReturn(singleton(new ImmutableErrorModel("test", "testNs", true, null)));
    return errorTypeRepository;
  }

  @Test
  public void retry() throws Throwable {
    stubException();
    Interceptor interceptor = mock(Interceptor.class);
    setInterceptors((Interceptable) configurationInstance, interceptor);
    setInterceptors((Interceptable) operationExecutor);

    defineOrder(interceptor);
    assertException(exception -> {
      assertThat(exception, instanceOf(ConnectionException.class));
      try {
        verify(interceptor, times(RETRY_COUNT + 1)).before(operationContext);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      verify(interceptor, times(RETRY_COUNT + 1)).onError(same(operationContext), anyVararg());
      verify(interceptor, times(RETRY_COUNT + 1)).after(operationContext, null);
    });
  }

  private void assertException(Consumer<Throwable> assertion) throws Throwable {
    try {
      execute().block();
      fail("was expecting a exception");
    } catch (Exception e) {
      assertion.accept(unwrap(e));
    }
  }

  private void stubExceptionOnBeforeInterceptor() throws Exception {
    doThrow(exception).when(operationInterceptor2).before(operationContext);
  }

  private void assertStatistics() {
    verify(configurationStats).addInflightOperation();
    verify(configurationStats).discountInflightOperation();
  }

  private void assertBefore() throws Exception {
    verifyInOrder(interceptor -> {
      try {
        interceptor.before(operationContext);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

    });
  }

  private void assertOnSuccess(VerificationMode verificationMode) {
    verifyInOrder(interceptor -> interceptor.onSuccess(operationContext, result), verificationMode);
  }

  private void assertOnError(VerificationMode verificationMode) {
    verifyInOrder(interceptor -> interceptor.onError(same(operationContext), same(connectionException)),
                  verificationMode);
  }

  private void assertAfter(Object expected) {
    verifyInOrder(interceptor -> interceptor.after(operationContext, expected));
  }

  private void assertResult(Object result) {
    assertThat(result, is(sameInstance(this.result)));
  }

  private void stubException() throws Exception {
    when(operationExecutor.execute(operationContext)).thenReturn(error(connectionException));
  }

  private void setInterceptors(Interceptable interceptable, Interceptor... interceptors) {
    when(interceptable.getInterceptors()).thenReturn(asList(interceptors));
  }

  private void defineOrder(Interceptor... interceptors) {
    inOrder = inOrder(interceptors);
    orderedInterceptors = ImmutableList.copyOf(interceptors);
  }

  private void verifyInOrder(Consumer<Interceptor> consumer) {
    verifyInOrder(consumer, times(1));
  }

  private void verifyInOrder(Consumer<Interceptor> consumer, VerificationMode verificationMode) {
    for (Interceptor interceptor : orderedInterceptors) {
      consumer.accept(inOrder.verify(interceptor, verificationMode));
    }
  }

  private Mono<Object> execute() throws MuleException {
    return from(mediator.execute(operationExecutor, operationContext));
  }
}
