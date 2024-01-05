/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor;

import static java.util.Arrays.asList;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_COMMIT;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_CONTINUE;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_START;
import static org.mule.runtime.core.api.construct.Flow.builder;
import static org.mule.runtime.core.api.transaction.Transaction.STATUS_ROLLEDBACK;
import static org.mule.runtime.core.internal.event.NullEventFactory.getNullEvent;
import static org.mule.runtime.core.internal.processor.TryScopeTestUtils.createTryScope;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static java.lang.Thread.sleep;

import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.internal.exception.OnErrorContinueHandler;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import javax.transaction.TransactionManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.concurrent.TimeoutException;

@RunWith(Parameterized.class)
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
  }

  @Before
  public void setup() throws Exception {
    when(profilingService.getProfilingDataProducer(TX_CONTINUE)).thenReturn(mock(ProfilingDataProducer.class));
    when(profilingService.getProfilingDataProducer(TX_START)).thenReturn(mock(ProfilingDataProducer.class));
    when(profilingService.getProfilingDataProducer(TX_COMMIT)).thenReturn(mock(ProfilingDataProducer.class));
    muleContext.setTransactionManager(manager);
  }

  @Test
  public void transactionIsRolledBackAfterTimeout() throws Exception {
    TryScope scope = createTryScope(true, muleContext, profilingService, isXa, TIMEOUT);
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
  public void transactionTimeoutErrorCanBeHandledOutsideFailingTry() throws Exception {
    // If we add the error handler to the failing TryScope, it won't work since the
    // tx is being attempted to be committed after the execution of its potential error handling
    // (i.e. no failure, or its on-error-continue). Therefore, a tx timeout error has to be handled in the
    // next level (a surrounding try, flow, etc...).
    TryScope scope = createTryScope(true, muleContext, profilingService, isXa, TIMEOUT);
    scope.setMessageProcessors(singletonList(new SleepyProcesssor()));

    TryScope outer = createTryScope(false, muleContext, profilingService);
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

  public static class SleepyProcesssor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) {
      Transaction tx = TransactionCoordination.getInstance().getTransaction();
      transaction = tx;
      try {
        sleep(EXECUTION_TIME);
        return event;
      } catch (InterruptedException e) {
        return event;
      }
    }
  }

}
