/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor;

import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_COMMIT;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_CONTINUE;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_START;
import static org.mule.runtime.core.api.construct.Flow.builder;
import static org.mule.runtime.core.api.transaction.Transaction.STATUS_ROLLEDBACK;
import static org.mule.runtime.core.internal.event.NullEventFactory.getNullEvent;
import static org.mule.runtime.core.internal.processor.TryScopeTestUtils.createPropagateErrorHandler;
import static org.mule.runtime.core.internal.processor.TryScopeTestUtils.createTryScope;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;
import static org.mule.test.allure.AllureConstants.TransactionFeature.TRANSACTION;
import static org.mule.test.allure.AllureConstants.TransactionFeature.TimeoutStory.TRANSACTION_TIMEOUT;

import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.internal.exception.OnErrorContinueHandler;
import org.mule.runtime.core.privileged.exception.TemplateOnErrorHandler;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Collection;
import java.util.concurrent.TimeoutException;

import javax.transaction.TransactionManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@RunWith(Parameterized.class)
@Feature(TRANSACTION)
@Story(TRANSACTION_TIMEOUT)
public class TransactionalTryTimeoutTestCase extends AbstractMuleContextTestCase {

  private static final int TIMEOUT = 1000;
  private static final int EXECUTION_TIME = 2 * TIMEOUT;

  private static Transaction transaction;
  private ProfilingService profilingService = mock(ProfilingService.class);
  private TransactionManager manager = mock(TransactionManager.class);
  private Flow flow;
  private boolean isXa;

  @Parameterized.Parameters(name = "isXa: {0}")
  public static Collection<Boolean> getParameters() {
    return asList(false, true);
  }

  public TransactionalTryTimeoutTestCase(Boolean isXa) {
    this.isXa = isXa;
  }

  @Before
  public void before() throws RegistrationException {
    flow = builder("flow", mockContextWithServices()).build();
    flow.setAnnotations(singletonMap(LOCATION_KEY, TEST_CONNECTOR_LOCATION));
    transaction = null;
  }

  @Before
  public void setup() throws Exception {
    when(profilingService.getProfilingDataProducer(TX_CONTINUE)).thenReturn(mock(ProfilingDataProducer.class));
    when(profilingService.getProfilingDataProducer(TX_START)).thenReturn(mock(ProfilingDataProducer.class));
    when(profilingService.getProfilingDataProducer(TX_COMMIT)).thenReturn(mock(ProfilingDataProducer.class));
  }

  @Test
  public void transactionIsRolledBackAfterTimeout() throws Exception {
    TryScope scope = createTryScope(muleContext, manager, profilingService, of(isXa), of(TIMEOUT));
    scope.setMessageProcessors(singletonList(new SleepyProcesssor()));
    scope.initialise();
    try {
      scope.process(getNullEvent());
      fail("Should have finished with a Tx Exception");
    } catch (TransactionException ex) {
      assertThat(ex.getCause(), instanceOf(TimeoutException.class));
      assertThat(transaction, is(notNullValue()));;
      assertThat(transaction.getStatus(), is(STATUS_ROLLEDBACK));
    } finally {
      scope.dispose();
    }
  }

  @Test
  public void markedAsRollbackExternallyWhenTimeout() throws Exception {
    TryScope scope = createTryScope(muleContext, manager, profilingService, of(isXa), of(TIMEOUT));
    scope.setMessageProcessors(singletonList(new SleepyProcesssor(true, false)));
    scope.initialise();
    try {
      scope.process(getNullEvent());
      fail("Should have finished with a Tx Exception");
    } catch (TransactionException ex) {
      assertThat(ex.getCause(), instanceOf(TimeoutException.class));
      assertThat(transaction, is(notNullValue()));;
      assertThat(transaction.getStatus(), is(STATUS_ROLLEDBACK));
    } finally {
      scope.dispose();
    }
  }

  @Test
  public void transactionTimeoutErrorCanBeHandledOutsideFailingTry() throws Exception {
    // If we add the error handler to the failing TryScope, it won't work since the
    // tx is being attempted to be committed after the execution of its potential error handling
    // (i.e. no failure, or its on-error-continue). Therefore, a tx timeout error has to be handled in the
    // next level (a surrounding try, flow, etc...).
    TryScope scope = createTryScope(muleContext, manager, profilingService, of(isXa), of(TIMEOUT));
    scope.setMessageProcessors(singletonList(new SleepyProcesssor()));

    TryScope outer = createTryScope(muleContext, manager, profilingService, empty());
    outer.setMessageProcessors(singletonList(scope));
    outer.setExceptionListener(new OnErrorContinueHandler());
    outer.initialise();
    try {
      outer.process(getNullEvent());
      assertThat(transaction, is(notNullValue()));
      assertThat(transaction.getStatus(), is(STATUS_ROLLEDBACK));
    } finally {
      outer.dispose();
    }
  }

  @Test
  public void originalErrorHasPrecedence() throws Exception {
    // If we end up having both an error and also a timeout (e.g. an operation that failed, did so because of
    // a connection issue, and it also exceded timeout), then the original error should have precedence over
    // the timeout, and it should be handled by the try's error handler.
    TryScope scope = createTryScope(muleContext, manager, profilingService, of(isXa), of(TIMEOUT));
    scope.setMessageProcessors(singletonList(new SleepyProcesssor(false, true)));
    TemplateOnErrorHandler handler = createPropagateErrorHandler();
    HandlerProcessor processor = new HandlerProcessor();
    handler.setMessageProcessors(singletonList(processor));
    scope.setExceptionListener(handler);
    scope.initialise();
    try {
      scope.process(getNullEvent());
      fail("Should have finished with a Tx Exception");
    } catch (MuleException ex) {
      assertThat(ex.getSuppressed().length, is(1));
      Throwable suppressed = ex.getSuppressed()[0];
      assertThat(suppressed, instanceOf(TransactionException.class));
      assertThat(suppressed.getCause(), instanceOf(TimeoutException.class));
      assertThat(transaction, is(notNullValue()));;
      assertThat(transaction.getStatus(), is(STATUS_ROLLEDBACK));
      assertThat(processor.executed(), is(true));
    } finally {
      scope.dispose();
    }
  }

  public static class SleepyProcesssor implements Processor {

    private final boolean markAsRollback;
    private final boolean raiseError;

    public SleepyProcesssor() {
      this(false, false);
    }

    public SleepyProcesssor(boolean markAsRollback, boolean raiseError) {
      this.markAsRollback = markAsRollback;
      this.raiseError = raiseError;
    }

    @Override
    public CoreEvent process(CoreEvent event) {
      Transaction tx = TransactionCoordination.getInstance().getTransaction();
      transaction = tx;
      try {
        sleep(EXECUTION_TIME);
        if (markAsRollback) {
          TransactionCoordination.getInstance().getTransaction().setRollbackOnly();
        }
        if (raiseError) {
          throw new MuleRuntimeException(createStaticMessage("some error"));
        }
        return event;
      } catch (InterruptedException | TransactionException e) {
        return event;
      }
    }
  }

  public static class HandlerProcessor implements Processor {

    private boolean wasExecuted = false;

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      wasExecuted = true;
      return event;
    }

    public boolean executed() {
      return wasExecuted;
    }
  }

}
