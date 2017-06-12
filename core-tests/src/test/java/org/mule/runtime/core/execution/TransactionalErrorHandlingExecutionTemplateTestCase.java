/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import static java.util.Optional.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_ALWAYS_BEGIN;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_ALWAYS_JOIN;
import static org.mule.runtime.core.api.transaction.TransactionConfig.ACTION_NONE;
import static org.mule.runtime.core.execution.TransactionalErrorHandlingExecutionTemplate.createMainExecutionTemplate;
import static org.mule.runtime.core.execution.TransactionalErrorHandlingExecutionTemplate.createScopeExecutionTemplate;
import static org.mule.runtime.core.transaction.TransactionTemplateTestUtils.getFailureTransactionCallbackStartsTransaction;

import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.api.transaction.ExternalTransactionAwareTransactionFactory;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.transaction.TransactionFactory;
import org.mule.runtime.core.context.notification.ServerNotificationManager;
import org.mule.runtime.core.exception.ErrorHandler;
import org.mule.runtime.core.exception.ErrorHandlerFactory;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.exception.OnErrorContinueHandler;
import org.mule.runtime.core.api.transaction.MuleTransactionConfig;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.transaction.TransactionTemplateTestUtils;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.mule.TestTransaction;
import org.mule.tck.testmodels.mule.TestTransactionFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class TransactionalErrorHandlingExecutionTemplateTestCase extends TransactionalExecutionTemplateTestCase {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private TransactionConfig mockTransactionConfig;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private TransactionFactory mockTransactionFactory;

  @Before
  public void setUp() throws TransactionException {
    when(mockTransactionConfig.getFactory()).thenReturn(mockTransactionFactory);
    when(mockTransactionFactory.beginTransaction(mockMuleContext)).thenAnswer(invocationOnMock -> {
      TransactionCoordination.getInstance().bindTransaction(mockTransaction);
      return mockTransaction;
    });
  }

  @Test
  public void testActionNoneAndXaTxAndFailureInCallback() throws Exception {
    mockTransaction.setXA(true);
    TransactionCoordination.getInstance().bindTransaction(mockTransaction);
    MuleTransactionConfig config = new MuleTransactionConfig(ACTION_NONE);
    ExecutionTemplate executionTemplate = createExecutionTemplate(config);
    Event mockExceptionListenerResultEvent = configureExceptionListenerCall();
    try {
      executionTemplate.execute(getFailureTransactionCallback());
      fail("MessagingException must be thrown");
    } catch (MessagingException e) {
      assertThat(e, is(mockMessagingException));
      verify(mockMessagingException).setProcessedEvent(mockExceptionListenerResultEvent);
    }

    verify(mockTransaction).suspend();
    verify(mockTransaction, times(0)).commit();
    verify(mockTransaction, times(0)).rollback();
    verify(mockTransaction).resume();
  }

  @Test
  public void testActionAlwaysBeginAndSuspendXaTxAndFailureCallback() throws Exception {
    TransactionCoordination.getInstance().bindTransaction(mockTransaction);
    MuleTransactionConfig config = createAlwaysBeginXaTransaction();
    ExecutionTemplate executionTemplate = createExecutionTemplate(config);
    config.setFactory(new TestTransactionFactory(mockNewTransaction));
    Event exceptionListenerResult = configureExceptionListenerCall();
    try {
      executionTemplate.execute(getFailureTransactionCallback());
    } catch (MessagingException e) {
      assertThat(e, is(mockMessagingException));
      assertThat(e.getEvent(), is(exceptionListenerResult));
    }
    verify(mockTransaction).suspend();
    verify(mockTransaction, times(0)).commit();
    verify(mockTransaction, times(0)).rollback();
    verify(mockNewTransaction).rollback();
    verify(mockTransaction).resume();
    assertThat((TestTransaction) TransactionCoordination.getInstance().getTransaction(), is(mockTransaction));
  }

  @Test
  public void testActionAlwaysJoinAndExternalTxAndFailureCallback() throws Exception {
    MuleTransactionConfig config = new MuleTransactionConfig(ACTION_ALWAYS_JOIN);
    config.setInteractWithExternal(true);
    mockExternalTransactionFactory = mock(ExternalTransactionAwareTransactionFactory.class);
    config.setFactory(mockExternalTransactionFactory);
    when(mockExternalTransactionFactory.joinExternalTransaction(mockMuleContext)).thenAnswer(invocationOnMock -> {
      TransactionCoordination.getInstance().bindTransaction(mockTransaction);
      return mockTransaction;
    });
    ExecutionTemplate transactionTemplate = createExecutionTemplate(config);
    Event exceptionListenerResult = configureExceptionListenerCall();
    try {
      transactionTemplate.execute(getFailureTransactionCallback());
    } catch (MessagingException e) {
      assertThat(e, is(mockMessagingException));
      assertThat(e.getEvent(), is(exceptionListenerResult));
    }
    verify(mockTransaction, times(0)).commit();
    verify(mockTransaction, times(1)).rollback();
    assertThat(TransactionCoordination.getInstance().getTransaction(), nullValue());
  }

  @Test
  public void testInnerTransactionCreatedAndResolved() throws Exception {
    ExecutionTemplate transactionTemplate = createExecutionTemplate(new MuleTransactionConfig());
    configureExceptionListenerCall();
    when(mockMessagingException.causedRollback()).thenReturn(false);
    try {
      transactionTemplate.execute(getFailureTransactionCallbackStartsTransaction(mockMessagingException, mockTransaction));
    } catch (MessagingException e) {
      assertThat(e, is(mockMessagingException));
    }
    verify(mockTransaction, times(0)).commit();
    verify(mockTransaction, times(1)).rollback();
    assertThat(TransactionCoordination.getInstance().getTransaction(), nullValue());
  }

  @Test
  public void testInnerTransactionCreatedAndNotResolved() throws Exception {
    ExecutionTemplate transactionTemplate =
        createScopeExecutionTemplate(mockMuleContext, mockFlow, new MuleTransactionConfig(), mockMessagingExceptionHandler);
    configureCatchExceptionListenerCall();
    when(mockMessagingException.causedRollback()).thenReturn(false);
    try {
      transactionTemplate.execute(getFailureTransactionCallbackStartsTransaction(mockMessagingException, mockTransaction));
    } catch (MessagingException e) {
      assertThat(e, is(mockMessagingException));
    }
    verify(mockTransaction, times(0)).commit();
    verify(mockTransaction, times(0)).rollback();
    assertThat(TransactionCoordination.getInstance().getTransaction(), notNullValue());
  }

  @Test
  public void transactionCommitFailsCallsExceptionHandler() throws Exception {
    when(mockTransactionConfig.getAction()).thenReturn(ACTION_ALWAYS_BEGIN);
    ExecutionTemplate executionTemplate = createExecutionTemplate(mockTransactionConfig);
    doAnswer(invocationOnMock -> {
      TransactionCoordination.getInstance().unbindTransaction(mockTransaction);
      throw new RuntimeException();
    }).when(mockTransaction).commit();
    configureCatchExceptionListenerCall();
    try {
      executionTemplate.execute(TransactionTemplateTestUtils.getEmptyTransactionCallback(mockEvent));
      fail("MessagingException must be thrown");
    } catch (MessagingException e) {
      assertThat(e.getEvent(), is(mockEvent));
    }
    verify(mockTransaction).commit();
    verify(mockMessagingExceptionHandler).handleException(any(MessagingException.class), eq(mockEvent));
  }

  @Override
  protected ExecutionTemplate createExecutionTemplate(TransactionConfig config) {
    return createMainExecutionTemplate(mockMuleContext, mockFlow, config, mockMessagingExceptionHandler);
  }

  private Event configureExceptionListenerCall() {
    final Event mockResultEvent = mock(Event.class, RETURNS_DEEP_STUBS.get());
    when(mockResultEvent.getError()).thenReturn(empty());
    when(mockMessagingException.getEvent()).thenReturn(mockEvent).thenReturn(mockResultEvent);
    when(mockEvent.getError()).thenReturn(empty());
    when(mockMessagingExceptionHandler.handleException(mockMessagingException, mockEvent)).thenAnswer(invocationOnMock -> {
      ErrorHandler errorHandler = new ErrorHandlerFactory().createDefault();
      when(mockMuleContext.getNotificationManager()).thenReturn(mock(ServerNotificationManager.class));
      when(mockMuleContext.getRegistry()).thenReturn(mock(MuleRegistry.class));
      errorHandler.setMuleContext(mockMuleContext);
      errorHandler.setFlowConstruct(mockFlow);
      errorHandler.handleException((MessagingException) invocationOnMock.getArguments()[0],
                                   (Event) invocationOnMock.getArguments()[1]);
      return mockResultEvent;
    });
    return mockResultEvent;
  }

  private Event configureCatchExceptionListenerCall() {
    final Event mockResultEvent = mock(Event.class, RETURNS_DEEP_STUBS.get());
    when(mockMessagingException.getEvent()).thenReturn(mockEvent).thenReturn(mockResultEvent);
    when(mockEvent.getError()).thenReturn(empty());
    when(mockResultEvent.getError()).thenReturn(empty());
    when(mockMessagingExceptionHandler.handleException(mockMessagingException, mockEvent)).thenAnswer(invocationOnMock -> {
      OnErrorContinueHandler exceptionStrategy = new OnErrorContinueHandler();
      exceptionStrategy.setMuleContext(mockMuleContext);
      exceptionStrategy.setFlowConstruct(mockFlow);
      when(mockMuleContext.getNotificationManager()).thenReturn(mock(ServerNotificationManager.class));
      when(mockMuleContext.getRegistry()).thenReturn(mock(MuleRegistry.class));
      exceptionStrategy.handleException((MessagingException) invocationOnMock.getArguments()[0],
                                        (Event) invocationOnMock.getArguments()[1]);
      return mockResultEvent;
    });
    return mockResultEvent;
  }

  protected ExecutionCallback<Event> getFailureTransactionCallback() throws Exception {
    return TransactionTemplateTestUtils.getFailureTransactionCallback(mockMessagingException);
  }

  private MuleTransactionConfig createAlwaysBeginXaTransaction() throws TransactionException {
    return createTransactionConfig(true);
  }

  private MuleTransactionConfig createTransactionConfig(boolean isXa) throws TransactionException {
    mockTransaction.setXA(isXa);
    return new MuleTransactionConfig(ACTION_ALWAYS_BEGIN);
  }

}
