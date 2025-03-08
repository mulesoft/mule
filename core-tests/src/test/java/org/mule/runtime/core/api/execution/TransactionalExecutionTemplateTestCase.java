/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.execution;

import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.getInstance;
import static org.mule.runtime.core.privileged.execution.TransactionalExecutionTemplate.createTransactionalExecutionTemplate;
import static org.mule.runtime.core.privileged.transaction.TransactionConfig.ACTION_ALWAYS_BEGIN;
import static org.mule.runtime.core.privileged.transaction.TransactionConfig.ACTION_ALWAYS_JOIN;
import static org.mule.runtime.core.privileged.transaction.TransactionConfig.ACTION_BEGIN_OR_JOIN;
import static org.mule.runtime.core.privileged.transaction.TransactionConfig.ACTION_INDIFFERENT;
import static org.mule.runtime.core.privileged.transaction.TransactionConfig.ACTION_JOIN_IF_POSSIBLE;
import static org.mule.runtime.core.privileged.transaction.TransactionConfig.ACTION_NEVER;
import static org.mule.runtime.core.privileged.transaction.TransactionConfig.ACTION_NONE;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThrows;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionTemplateTestUtils;
import org.mule.runtime.core.internal.transaction.MuleTransactionConfig;
import org.mule.runtime.core.internal.transaction.xa.IllegalTransactionStateException;
import org.mule.runtime.core.privileged.exception.MessagingException;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.core.privileged.transaction.TransactionConfig;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.mule.TestTransaction;
import org.mule.tck.testmodels.mule.TestTransactionFactory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@SmallTest
public class TransactionalExecutionTemplateTestCase extends AbstractMuleTestCase {

  @Rule
  public MockitoRule rule = MockitoJUnit.rule();

  @Mock
  protected CoreEvent RETURN_VALUE;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private MuleConfiguration configuration;

  @Mock
  private NotificationDispatcher notificationDispatcher;

  private final String applicationName = "appName";

  private TestTransaction mockTransaction;

  private TestTransaction mockNewTransaction;

  @Mock(lenient = true)
  private MessagingException mockMessagingException;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private CoreEvent mockEvent;

  @Before
  public void prepareEvent() throws RegistrationException {
    when(mockEvent.getMessage()).thenReturn(of(""));

    mockTransaction =
        spy(new TestTransaction(applicationName, notificationDispatcher));
    mockNewTransaction =
        spy(new TestTransaction(applicationName, notificationDispatcher));
  }

  @Before
  public void unbindTransaction() throws Exception {
    Transaction currentTransaction = getInstance().getTransaction();
    if (currentTransaction != null) {
      getInstance().unbindTransaction(currentTransaction);
    }
    when(mockMessagingException.getStackTrace()).thenReturn(new StackTraceElement[0]);
  }

  @Test
  public void testActionIndifferentConfig() throws Exception {
    MuleTransactionConfig config = new MuleTransactionConfig(ACTION_INDIFFERENT);
    ExecutionTemplate<CoreEvent> executionTemplate = createExecutionTemplate(config);
    Object result = executionTemplate.execute(getEmptyTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
    assertThat(getInstance().getTransaction(), nullValue());
  }

  @Test
  public void testActionNeverAndNoTx() throws Exception {
    MuleTransactionConfig config = new MuleTransactionConfig(ACTION_NEVER);
    ExecutionTemplate<CoreEvent> executionTemplate = createExecutionTemplate(config);
    Object result = executionTemplate.execute(getEmptyTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
  }

  @Test(expected = IllegalTransactionStateException.class)
  public void testActionNeverAndTx() throws Exception {
    getInstance().bindTransaction(mockTransaction);
    MuleTransactionConfig config = new MuleTransactionConfig(ACTION_NEVER);
    ExecutionTemplate<CoreEvent> executionTemplate = createExecutionTemplate(config);
    executionTemplate.execute(getEmptyTransactionCallback());
  }

  @Test
  public void testActionNoneAndNoTx() throws Exception {
    MuleTransactionConfig config = new MuleTransactionConfig(ACTION_NONE);
    ExecutionTemplate<CoreEvent> executionTemplate = createExecutionTemplate(config);
    Object result = executionTemplate.execute(getEmptyTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
  }

  @Test
  public void testActionNoneAndTxForCommit() throws Exception {
    getInstance().bindTransaction(mockTransaction);
    MuleTransactionConfig config = new MuleTransactionConfig(ACTION_NONE);
    ExecutionTemplate<CoreEvent> executionTemplate = createExecutionTemplate(config);
    Object result = executionTemplate.execute(getEmptyTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
    verify(mockTransaction, never()).commit();
    verify(mockTransaction, never()).rollback();
  }

  @Test
  public void testActionNoneAndTxForRollback() throws Exception {
    getInstance().bindTransaction(mockTransaction);
    MuleTransactionConfig config = new MuleTransactionConfig(ACTION_NONE);
    ExecutionTemplate<CoreEvent> executionTemplate = createExecutionTemplate(config);
    Object result = executionTemplate.execute(getEmptyTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
    verify(mockTransaction, never()).commit();
    verify(mockTransaction, never()).rollback();
  }

  @Test
  public void testActionNoneAndXaTx() throws Exception {
    mockTransaction.setXA(true);
    getInstance().bindTransaction(mockTransaction);
    MuleTransactionConfig config = new MuleTransactionConfig(ACTION_NONE);
    ExecutionTemplate<CoreEvent> executionTemplate = createExecutionTemplate(config);
    Object result = executionTemplate.execute(getEmptyTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
    verify(mockTransaction).suspend();
    verify(mockTransaction).resume();
    verify(mockTransaction, never()).commit();
    verify(mockTransaction, never()).rollback();
  }

  @Test
  public void testActionAlwaysBeginAndCommitTxAndCommitNewTx() throws Exception {
    getInstance().bindTransaction(mockTransaction);
    MuleTransactionConfig config = new MuleTransactionConfig(ACTION_ALWAYS_BEGIN);
    ExecutionTemplate<CoreEvent> executionTemplate = createExecutionTemplate(config);
    config.setFactory(new TestTransactionFactory(mockNewTransaction));

    assertThrows(IllegalTransactionStateException.class, () -> executionTemplate.execute(getEmptyTransactionCallback()));
  }

  @Test(expected = IllegalTransactionStateException.class)
  public void testActionAlwaysBeginAndRollbackTxAndCommitNewTx() throws Exception {
    getInstance().bindTransaction(mockTransaction);
    MuleTransactionConfig config = new MuleTransactionConfig(ACTION_ALWAYS_BEGIN);
    ExecutionTemplate<CoreEvent> executionTemplate = createExecutionTemplate(config);
    config.setFactory(new TestTransactionFactory(mockNewTransaction));
    Object result = executionTemplate.execute(getEmptyTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
    verify(mockTransaction).rollback();
    verify(mockNewTransaction).commit();
    verify(mockTransaction, never()).commit();
    verify(mockNewTransaction, never()).rollback();
    assertThat(getInstance().getTransaction(), nullValue());
  }

  @Test(expected = IllegalTransactionStateException.class)
  public void testActionAlwaysBeginAndRollbackTxAndRollbackNewTx() throws Exception {
    getInstance().bindTransaction(mockTransaction);
    MuleTransactionConfig config = new MuleTransactionConfig(ACTION_ALWAYS_BEGIN);
    ExecutionTemplate<CoreEvent> executionTemplate = createExecutionTemplate(config);
    config.setFactory(new TestTransactionFactory(mockNewTransaction));
    Object result = executionTemplate.execute(getRollbackTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
    verify(mockTransaction).rollback();
    verify(mockNewTransaction).rollback();
    verify(mockTransaction, never()).commit();
    verify(mockNewTransaction, never()).commit();
    assertThat(getInstance().getTransaction(), nullValue());
  }

  @Test
  public void testActionAlwaysBeginAndSuspendXaTxAndCommitNewTx() throws Exception {
    getInstance().bindTransaction(mockTransaction);
    mockTransaction.setXA(true);
    MuleTransactionConfig config = new MuleTransactionConfig(ACTION_ALWAYS_BEGIN);
    ExecutionTemplate<CoreEvent> executionTemplate = createExecutionTemplate(config);
    config.setFactory(new TestTransactionFactory(mockNewTransaction));
    Object result = executionTemplate.execute(getEmptyTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
    verify(mockNewTransaction).commit();
    verify(mockNewTransaction, never()).rollback();
    verify(mockTransaction).suspend();
    verify(mockTransaction).resume();
    verify(mockTransaction, never()).commit();
    verify(mockTransaction, never()).rollback();
    assertThat(getInstance().getTransaction(), is(mockTransaction));
  }

  @Test
  public void testActionAlwaysBeginAndSuspendXaTxAndRollbackNewTx() throws Exception {
    getInstance().bindTransaction(mockTransaction);
    mockTransaction.setXA(true);
    MuleTransactionConfig config = new MuleTransactionConfig(ACTION_ALWAYS_BEGIN);
    ExecutionTemplate<CoreEvent> executionTemplate = createExecutionTemplate(config);
    config.setFactory(new TestTransactionFactory(mockNewTransaction));
    Object result = executionTemplate.execute(getRollbackTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
    verify(mockNewTransaction).rollback();
    verify(mockNewTransaction, never()).commit();
    verify(mockTransaction).suspend();
    verify(mockTransaction).resume();
    verify(mockTransaction, never()).commit();
    verify(mockTransaction, never()).rollback();
    assertThat(getInstance().getTransaction(), is(mockTransaction));
  }

  @Test(expected = IllegalTransactionStateException.class)
  public void testActionAlwaysJoinAndNoTx() throws Exception {
    MuleTransactionConfig config = new MuleTransactionConfig(ACTION_ALWAYS_JOIN);
    ExecutionTemplate<CoreEvent> executionTemplate = createExecutionTemplate(config);
    executionTemplate.execute(getRollbackTransactionCallback());
  }

  @Test
  public void testActionAlwaysJoinAndTx() throws Exception {
    getInstance().bindTransaction(mockTransaction);
    MuleTransactionConfig config = new MuleTransactionConfig(ACTION_ALWAYS_JOIN);
    ExecutionTemplate<CoreEvent> executionTemplate = createExecutionTemplate(config);
    Object result = executionTemplate.execute(getRollbackTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
    verify(mockTransaction, never()).commit();
    verify(mockTransaction, never()).rollback();
    assertThat(getInstance().getTransaction(), is(mockTransaction));
  }

  @Test
  public void testActionBeginOrJoinAndNoTx() throws Exception {
    MuleTransactionConfig config = new MuleTransactionConfig(ACTION_BEGIN_OR_JOIN);
    ExecutionTemplate<CoreEvent> executionTemplate = createExecutionTemplate(config);
    config.setFactory(new TestTransactionFactory(mockTransaction));
    Object result = executionTemplate.execute(getEmptyTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
    verify(mockTransaction).commit();
    verify(mockTransaction, never()).rollback();
    assertThat(getInstance().getTransaction(), nullValue());
  }

  @Test
  public void testActionBeginOrJoinAndTx() throws Exception {
    getInstance().bindTransaction(mockTransaction);
    MuleTransactionConfig config = new MuleTransactionConfig(ACTION_BEGIN_OR_JOIN);
    ExecutionTemplate<CoreEvent> executionTemplate = createExecutionTemplate(config);
    config.setFactory(new TestTransactionFactory(mockTransaction));
    Object result = executionTemplate.execute(getEmptyTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
    verify(mockTransaction, never()).commit();
    verify(mockTransaction, never()).rollback();
    assertThat(getInstance().getTransaction(), is(mockTransaction));
  }

  @Test
  public void testActionJoinIfPossibleAndNoTx() throws Exception {
    MuleTransactionConfig config = new MuleTransactionConfig(ACTION_JOIN_IF_POSSIBLE);
    ExecutionTemplate<CoreEvent> executionTemplate = createExecutionTemplate(config);
    Object result = executionTemplate.execute(getEmptyTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
    assertThat(getInstance().getTransaction(), nullValue());
  }

  @Test
  public void testActionJoinIfPossibleAndTx() throws Exception {
    getInstance().bindTransaction(mockTransaction);
    MuleTransactionConfig config = new MuleTransactionConfig(ACTION_JOIN_IF_POSSIBLE);
    ExecutionTemplate<CoreEvent> executionTemplate = createExecutionTemplate(config);
    Object result = executionTemplate.execute(getEmptyTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
    verify(mockTransaction, never()).commit();
    verify(mockTransaction, never()).rollback();
    assertThat(getInstance().getTransaction(), is(mockTransaction));
  }

  protected ExecutionTemplate<CoreEvent> createExecutionTemplate(TransactionConfig config) {
    return createTransactionalExecutionTemplate(configuration,
                                                notificationDispatcher,
                                                config);
  }

  protected ExecutionCallback<CoreEvent> getEmptyTransactionCallback() {
    return TransactionTemplateTestUtils.getEmptyTransactionCallback(RETURN_VALUE);
  }

  protected ExecutionCallback<CoreEvent> getRollbackTransactionCallback() {
    return TransactionTemplateTestUtils.getRollbackTransactionCallback(RETURN_VALUE);
  }

}
