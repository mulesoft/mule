/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import static java.util.Optional.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.execution.ErrorHandlingExecutionTemplate.createErrorHandlingExecutionTemplate;
import static org.mule.runtime.core.transaction.TransactionTemplateTestUtils.getEmptyTransactionCallback;
import static org.mule.runtime.core.transaction.TransactionTemplateTestUtils.getFailureTransactionCallback;
import static org.mule.runtime.core.transaction.TransactionTemplateTestUtils.getFailureTransactionCallbackStartsTransaction;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.InternalMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.context.notification.ServerNotificationManager;
import org.mule.runtime.core.exception.DefaultMessagingExceptionStrategy;
import org.mule.runtime.core.routing.filters.WildcardFilter;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.mule.TestTransaction;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class ErrorHandlingExecutionTemplateTestCase extends AbstractMuleTestCase {

  private MuleContext mockMuleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
  private FlowConstruct mockFlow = mock(FlowConstruct.class, RETURNS_DEEP_STUBS);
  @Mock
  private Event RETURN_VALUE;
  @Mock
  private MessagingException mockMessagingException;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private Event mockEvent;
  @Spy
  protected TestTransaction mockTransaction = new TestTransaction(mockMuleContext);
  @Mock
  protected MessagingExceptionHandler mockMessagingExceptionHandler;


  @Before
  public void unbindTransaction() throws Exception {
    when(mockEvent.getMessage()).thenReturn(InternalMessage.builder().payload("").build());

    Transaction currentTransaction = TransactionCoordination.getInstance().getTransaction();
    if (currentTransaction != null) {
      TransactionCoordination.getInstance().unbindTransaction(currentTransaction);
    }
    when(mockMessagingException.getStackTrace()).thenReturn(new StackTraceElement[0]);
    when(mockEvent.getError()).thenReturn(empty());
  }

  @Test
  public void testSuccessfulExecution() throws Exception {
    ExecutionTemplate executionTemplate = createExceptionHandlingTransactionTemplate();
    Object result = executionTemplate.execute(getEmptyTransactionCallback(RETURN_VALUE));
    assertThat((Event) result, is(RETURN_VALUE));
  }

  private ExecutionTemplate createExceptionHandlingTransactionTemplate() {
    return createErrorHandlingExecutionTemplate(mockMuleContext, mockFlow, mockMessagingExceptionHandler);
  }

  @Test
  public void testFailureException() throws Exception {
    ExecutionTemplate executionTemplate = createExceptionHandlingTransactionTemplate();
    Event mockResultEvent = mock(Event.class);
    when(mockMessagingException.getEvent()).thenReturn(mockEvent).thenReturn(mockEvent).thenReturn(mockResultEvent);
    when(mockMessagingExceptionHandler.handleException(mockMessagingException, mockEvent)).thenReturn(mockResultEvent);
    try {
      executionTemplate.execute(getFailureTransactionCallback(mockMessagingException));
      fail("MessagingException must be thrown");
    } catch (MessagingException e) {
      assertThat(e, Is.is(mockMessagingException));
      verify(mockMessagingException).setProcessedEvent(mockResultEvent);
    }
  }

  @Test
  public void testTransactionIsMarkedRollbackOnExceptionByDefault() throws Exception {
    ExecutionTemplate executionTemplate = createExceptionHandlingTransactionTemplate();
    TransactionCoordination.getInstance().bindTransaction(mockTransaction);
    configureExceptionListener(null, null);
    try {
      executionTemplate.execute(getFailureTransactionCallback(mockMessagingException));
      fail("MessagingException must be thrown");
    } catch (MessagingException e) {
    }
    verify(mockTransaction).rollback();
  }

  @Test
  public void testTransactionIsNotRollbackOnEveryException() throws Exception {
    ExecutionTemplate executionTemplate = createExceptionHandlingTransactionTemplate();
    TransactionCoordination.getInstance().bindTransaction(mockTransaction);
    configureExceptionListener(null, "*");
    try {
      executionTemplate.execute(getFailureTransactionCallback(mockMessagingException));
      fail("MessagingException must be thrown");
    } catch (MessagingException e) {
    }
    verify(mockTransaction, times(0)).setRollbackOnly();
    verify(mockTransaction, times(0)).commit();
    verify(mockTransaction, times(0)).rollback();
  }

  @Test
  public void testTransactionIsNotRollbackOnMatcherRegexPatternException() throws Exception {
    ExecutionTemplate executionTemplate = createExceptionHandlingTransactionTemplate();
    TransactionCoordination.getInstance().bindTransaction(mockTransaction);
    configureExceptionListener(null, "org.mule.runtime.core.excep*");
    try {
      executionTemplate.execute(getFailureTransactionCallback(mockMessagingException));
      fail("MessagingException must be thrown");
    } catch (MessagingException e) {
    }
    verify(mockTransaction, times(0)).setRollbackOnly();
    verify(mockTransaction, times(0)).commit();
    verify(mockTransaction, times(0)).rollback();
  }

  @Test
  public void testTransactionIsNotRollbackOnClassHierarchyPatternException() throws Exception {
    ExecutionTemplate executionTemplate = createExceptionHandlingTransactionTemplate();
    TransactionCoordination.getInstance().bindTransaction(mockTransaction);
    configureExceptionListener(null, "org.mule.runtime.core.api.MuleException+");
    try {
      executionTemplate.execute(getFailureTransactionCallback(mockMessagingException));
      fail("MessagingException must be thrown");
    } catch (MessagingException e) {
    }
    verify(mockTransaction, times(0)).setRollbackOnly();
    verify(mockTransaction, times(0)).commit();
    verify(mockTransaction, times(0)).rollback();
  }

  @Test
  public void testTransactionIsNotRollbackOnClassExactlyPatternException() throws Exception {
    ExecutionTemplate executionTemplate = createExceptionHandlingTransactionTemplate();
    TransactionCoordination.getInstance().bindTransaction(mockTransaction);
    configureExceptionListener(null, "org.mule.runtime.core.exception.MessagingException");
    try {
      executionTemplate.execute(getFailureTransactionCallback(new MessagingException(mockEvent, null)));
      fail("MessagingException must be thrown");
    } catch (MessagingException e) {
    }
    verify(mockTransaction, times(0)).setRollbackOnly();
    verify(mockTransaction, times(0)).commit();
    verify(mockTransaction, times(0)).rollback();
  }

  @Test
  public void testTransactionIsRollbackOnPatternAppliesToRollbackAndCommit() throws Exception {
    ExecutionTemplate executionTemplate = createExceptionHandlingTransactionTemplate();
    TransactionCoordination.getInstance().bindTransaction(mockTransaction);
    configureExceptionListener("org.mule.runtime.core.api.MuleException+", "org.mule.runtime.core.exception.MessagingException");
    try {
      executionTemplate.execute(getFailureTransactionCallback(mockMessagingException));
      fail("MessagingException must be thrown");
    } catch (MessagingException e) {
    }
    verify(mockTransaction, times(1)).setRollbackOnly();
    verify(mockTransaction, times(0)).commit();
    verify(mockTransaction, times(1)).rollback();
  }

  @Test
  public void testSuspendedTransactionNotResumedOnException() throws Exception {
    mockTransaction.setXA(true);
    TransactionCoordination.getInstance().bindTransaction(mockTransaction);
    TransactionCoordination.getInstance().suspendCurrentTransaction();
    assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    configureExceptionListener(null, null);
    ExecutionTemplate executionTemplate = createExceptionHandlingTransactionTemplate();
    try {
      executionTemplate.execute(getFailureTransactionCallback(mockMessagingException));
      fail("MessagingException must be thrown");
    } catch (MessagingException e) {
    }
    verify(mockTransaction, times(0)).resume();
    verify(mockTransaction, times(0)).rollback();
    verify(mockTransaction, times(0)).commit();
    verify(mockTransaction, times(0)).setRollbackOnly();
    assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
  }

  @Test
  public void testSuspendedTransactionNotResumedAndNewTransactionResolvedOnException() throws Exception {
    mockTransaction.setXA(true);
    TransactionCoordination.getInstance().bindTransaction(mockTransaction);
    TransactionCoordination.getInstance().suspendCurrentTransaction();
    assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
    configureExceptionListener(null, null);
    ExecutionTemplate executionTemplate = createExceptionHandlingTransactionTemplate();
    final Transaction mockNewTransaction = spy(new TestTransaction(mockMuleContext));
    try {
      executionTemplate.execute(getFailureTransactionCallbackStartsTransaction(mockMessagingException, mockNewTransaction));
      fail("MessagingException must be thrown");
    } catch (MessagingException e) {
    }
    verify(mockTransaction, times(0)).resume();
    verify(mockTransaction, times(0)).rollback();
    verify(mockTransaction, times(0)).commit();
    verify(mockTransaction, times(0)).setRollbackOnly();
    verify(mockNewTransaction, times(1)).rollback();
    verify(mockNewTransaction, times(0)).commit();
    verify(mockNewTransaction, times(1)).setRollbackOnly();
    assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
  }

  @Test
  public void testTransactionIsResolved() throws Exception {
    configureExceptionListener(null, null);
    ExecutionTemplate executionTemplate = createExceptionHandlingTransactionTemplate();
    try {
      executionTemplate.execute(getFailureTransactionCallbackStartsTransaction(mockMessagingException, mockTransaction));
      fail("MessagingException must be thrown");
    } catch (MessagingException e) {
    }
    verify(mockTransaction, times(1)).setRollbackOnly();
    verify(mockTransaction, times(1)).rollback();
    verify(mockTransaction, times(0)).commit();
    assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
  }

  private void configureExceptionListener(final String rollbackFilter, final String commitFilter) {
    when(mockMessagingException.getEvent()).thenReturn(mockEvent);
    when(mockMuleContext.getNotificationManager()).thenReturn(mock(ServerNotificationManager.class));
    when(mockMuleContext.getRegistry()).thenReturn(mock(MuleRegistry.class));
    when(mockMessagingExceptionHandler.handleException(any(MessagingException.class), any(Event.class)))
        .thenAnswer(invocationOnMock -> {
          DefaultMessagingExceptionStrategy defaultMessagingExceptionStrategy = new DefaultMessagingExceptionStrategy();
          defaultMessagingExceptionStrategy.setMuleContext(mockMuleContext);
          defaultMessagingExceptionStrategy.setFlowConstruct(mockFlow);
          if (rollbackFilter != null) {
            defaultMessagingExceptionStrategy.setRollbackTxFilter(new WildcardFilter(rollbackFilter));
          }
          if (commitFilter != null) {
            defaultMessagingExceptionStrategy.setCommitTxFilter(new WildcardFilter(commitFilter));
          }
          defaultMessagingExceptionStrategy.handleException((MessagingException) invocationOnMock.getArguments()[0],
                                                            (Event) invocationOnMock.getArguments()[1]);
          return null;
        });
  }


}
