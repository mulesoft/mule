/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.execution;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.transaction.ExternalTransactionAwareTransactionFactory;
import org.mule.runtime.core.api.transaction.MuleTransactionConfig;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.runtime.core.api.transaction.TransactionTemplateTestUtils;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.core.privileged.transaction.xa.IllegalTransactionStateException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.mule.TestTransaction;
import org.mule.tck.testmodels.mule.TestTransactionFactory;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class TransactionalExecutionTemplateTestCase extends AbstractMuleTestCase {

  protected MuleContext mockMuleContext = mockContextWithServices();

  protected FlowConstruct mockFlow =
      mock(FlowConstruct.class, withSettings().extraInterfaces(Component.class).defaultAnswer(RETURNS_DEEP_STUBS));
  @Mock
  protected CoreEvent RETURN_VALUE;
  @Spy
  protected TestTransaction mockTransaction = new TestTransaction(mockMuleContext);
  @Spy
  protected TestTransaction mockNewTransaction = new TestTransaction(mockMuleContext);
  @Mock
  protected ExternalTransactionAwareTransactionFactory mockExternalTransactionFactory;
  @Mock
  protected MessagingException mockMessagingException;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  protected CoreEvent mockEvent;
  @Mock
  protected FlowExceptionHandler mockMessagingExceptionHandler;

  @Rule
  public ExpectedException expectedException = none();

  @Before
  public void prepareEvent() throws RegistrationException {
    when(mockEvent.getMessage()).thenReturn(of(""));
  }

  @Before
  public void unbindTransaction() throws Exception {
    Transaction currentTransaction = TransactionCoordination.getInstance().getTransaction();
    if (currentTransaction != null) {
      TransactionCoordination.getInstance().unbindTransaction(currentTransaction);
    }
    when(mockMessagingException.getStackTrace()).thenReturn(new StackTraceElement[0]);
  }

  @Test
  public void testActionIndifferentConfig() throws Exception {
    MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_INDIFFERENT);
    ExecutionTemplate executionTemplate = createExecutionTemplate(config);
    Object result = executionTemplate.execute(getEmptyTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
    assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
  }

  @Test
  public void testActionNeverAndNoTx() throws Exception {
    MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NEVER);
    ExecutionTemplate executionTemplate = createExecutionTemplate(config);
    Object result = executionTemplate.execute(getEmptyTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
  }

  @Test(expected = IllegalTransactionStateException.class)
  public void testActionNeverAndTx() throws Exception {
    TransactionCoordination.getInstance().bindTransaction(mockTransaction);
    MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NEVER);
    ExecutionTemplate executionTemplate = createExecutionTemplate(config);
    executionTemplate.execute(getEmptyTransactionCallback());
  }

  @Test
  public void testActionNoneAndNoTx() throws Exception {
    MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NONE);
    ExecutionTemplate executionTemplate = createExecutionTemplate(config);
    Object result = executionTemplate.execute(getEmptyTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
  }

  @Test
  public void testActionNoneAndTxForCommit() throws Exception {
    TransactionCoordination.getInstance().bindTransaction(mockTransaction);
    MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NONE);
    ExecutionTemplate executionTemplate = createExecutionTemplate(config);
    Object result = executionTemplate.execute(getEmptyTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
    verify(mockTransaction, never()).commit();
    verify(mockTransaction, never()).rollback();
  }


  @Test
  public void testActionNoneAndTxForRollback() throws Exception {
    when(mockTransaction.isRollbackOnly()).thenReturn(true);
    TransactionCoordination.getInstance().bindTransaction(mockTransaction);
    MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NONE);
    ExecutionTemplate executionTemplate = createExecutionTemplate(config);
    Object result = executionTemplate.execute(getEmptyTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
    verify(mockTransaction, never()).commit();
    verify(mockTransaction, never()).rollback();
  }

  @Test
  public void testActionNoneAndXaTx() throws Exception {
    mockTransaction.setXA(true);
    TransactionCoordination.getInstance().bindTransaction(mockTransaction);
    MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NONE);
    ExecutionTemplate executionTemplate = createExecutionTemplate(config);
    Object result = executionTemplate.execute(getEmptyTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
    verify(mockTransaction).suspend();
    verify(mockTransaction).resume();
    verify(mockTransaction, never()).commit();
    verify(mockTransaction, never()).rollback();
  }


  @Test
  public void testActionNoneAndWithExternalTransactionWithNoTx() throws Exception {
    MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NONE);
    config.setInteractWithExternal(true);
    mockExternalTransactionFactory = mock(ExternalTransactionAwareTransactionFactory.class);
    config.setFactory(mockExternalTransactionFactory);
    when(mockExternalTransactionFactory.joinExternalTransaction(mockMuleContext)).thenAnswer(invocationOnMock -> {
      TransactionCoordination.getInstance().bindTransaction(mockTransaction);
      return mockTransaction;
    });
    mockTransaction.setXA(true);
    ExecutionTemplate executionTemplate = createExecutionTemplate(config);
    Object result = executionTemplate.execute(getEmptyTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
    verify(mockTransaction).suspend();
    verify(mockTransaction).resume();
    verify(mockTransaction, never()).commit();
    verify(mockTransaction, never()).rollback();
    assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
  }

  @Test
  public void testActionNoneAndWithExternalTransactionWithTx() throws Exception {
    TransactionCoordination.getInstance().bindTransaction(mockTransaction);
    MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_NONE);
    config.setInteractWithExternal(true);
    mockExternalTransactionFactory = mock(ExternalTransactionAwareTransactionFactory.class);
    config.setFactory(mockExternalTransactionFactory);
    Transaction externalTransaction = mock(Transaction.class);
    when(mockExternalTransactionFactory.joinExternalTransaction(mockMuleContext)).thenReturn(externalTransaction);
    ExecutionTemplate executionTemplate = createExecutionTemplate(config);
    Object result = executionTemplate.execute(getEmptyTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
    verify(mockTransaction, never()).rollback();
    verify(mockTransaction, never()).commit();
    assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>notNullValue());
  }

  @Test
  public void testActionAlwaysBeginAndCommitTxAndCommitNewTx() throws Exception {
    expectedException.expect(IllegalTransactionStateException.class);
    TransactionCoordination.getInstance().bindTransaction(mockTransaction);
    MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_BEGIN);
    ExecutionTemplate executionTemplate = createExecutionTemplate(config);
    config.setFactory(new TestTransactionFactory(mockNewTransaction));
    executionTemplate.execute(getEmptyTransactionCallback());
  }

  @Test(expected = IllegalTransactionStateException.class)
  public void testActionAlwaysBeginAndRollbackTxAndCommitNewTx() throws Exception {
    TransactionCoordination.getInstance().bindTransaction(mockTransaction);
    when(mockTransaction.isRollbackOnly()).thenReturn(true);
    MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_BEGIN);
    ExecutionTemplate executionTemplate = createExecutionTemplate(config);
    config.setFactory(new TestTransactionFactory(mockNewTransaction));
    Object result = executionTemplate.execute(getEmptyTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
    verify(mockTransaction).rollback();
    verify(mockNewTransaction).commit();
    verify(mockTransaction, never()).commit();
    verify(mockNewTransaction, never()).rollback();
    assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
  }

  @Test(expected = IllegalTransactionStateException.class)
  public void testActionAlwaysBeginAndRollbackTxAndRollbackNewTx() throws Exception {
    TransactionCoordination.getInstance().bindTransaction(mockTransaction);
    when(mockTransaction.isRollbackOnly()).thenReturn(true);
    MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_BEGIN);
    ExecutionTemplate executionTemplate = createExecutionTemplate(config);
    config.setFactory(new TestTransactionFactory(mockNewTransaction));
    Object result = executionTemplate.execute(getRollbackTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
    verify(mockTransaction).rollback();
    verify(mockNewTransaction).rollback();
    verify(mockTransaction, never()).commit();
    verify(mockNewTransaction, never()).commit();
    assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
  }

  @Test
  public void testActionAlwaysBeginAndSuspendXaTxAndCommitNewTx() throws Exception {
    TransactionCoordination.getInstance().bindTransaction(mockTransaction);
    mockTransaction.setXA(true);
    MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_BEGIN);
    ExecutionTemplate executionTemplate = createExecutionTemplate(config);
    config.setFactory(new TestTransactionFactory(mockNewTransaction));
    Object result = executionTemplate.execute(getEmptyTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
    verify(mockNewTransaction).commit();
    verify(mockNewTransaction, never()).rollback();
    verify(mockTransaction).suspend();
    verify(mockTransaction).resume();
    verify(mockTransaction, never()).commit();
    verify(mockTransaction, never()).rollback();
    assertThat((TestTransaction) TransactionCoordination.getInstance().getTransaction(), is(mockTransaction));
  }

  @Test
  public void testActionAlwaysBeginAndSuspendXaTxAndRollbackNewTx() throws Exception {
    TransactionCoordination.getInstance().bindTransaction(mockTransaction);
    mockTransaction.setXA(true);
    when(mockTransaction.isRollbackOnly()).thenReturn(true);
    MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_BEGIN);
    ExecutionTemplate executionTemplate = createExecutionTemplate(config);
    config.setFactory(new TestTransactionFactory(mockNewTransaction));
    Object result = executionTemplate.execute(getRollbackTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
    verify(mockNewTransaction).rollback();
    verify(mockNewTransaction, never()).commit();
    verify(mockTransaction).suspend();
    verify(mockTransaction).resume();
    verify(mockTransaction, never()).commit();
    verify(mockTransaction, never()).rollback();
    assertThat((TestTransaction) TransactionCoordination.getInstance().getTransaction(), is(mockTransaction));
  }



  @Test(expected = IllegalTransactionStateException.class)
  public void testActionAlwaysJoinAndNoTx() throws Exception {
    MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_JOIN);
    ExecutionTemplate executionTemplate = createExecutionTemplate(config);
    executionTemplate.execute(getRollbackTransactionCallback());
  }

  @Test
  public void testActionAlwaysJoinAndTx() throws Exception {
    TransactionCoordination.getInstance().bindTransaction(mockTransaction);
    MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_JOIN);
    ExecutionTemplate executionTemplate = createExecutionTemplate(config);
    Object result = executionTemplate.execute(getRollbackTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
    verify(mockTransaction, never()).commit();
    verify(mockTransaction, never()).rollback();
    assertThat((TestTransaction) TransactionCoordination.getInstance().getTransaction(), is(mockTransaction));
  }


  @Test
  public void testActionBeginOrJoinAndNoTx() throws Exception {
    MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_BEGIN_OR_JOIN);
    ExecutionTemplate executionTemplate = createExecutionTemplate(config);
    config.setFactory(new TestTransactionFactory(mockTransaction));
    Object result = executionTemplate.execute(getEmptyTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
    verify(mockTransaction).commit();
    verify(mockTransaction, never()).rollback();
    assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
  }

  @Test
  public void testActionBeginOrJoinAndTx() throws Exception {
    TransactionCoordination.getInstance().bindTransaction(mockTransaction);
    MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_BEGIN_OR_JOIN);
    ExecutionTemplate executionTemplate = createExecutionTemplate(config);
    config.setFactory(new TestTransactionFactory(mockTransaction));
    Object result = executionTemplate.execute(getEmptyTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
    verify(mockTransaction, never()).commit();
    verify(mockTransaction, never()).rollback();
    assertThat((TestTransaction) TransactionCoordination.getInstance().getTransaction(), is(mockTransaction));
  }

  @Test
  public void testActionJoinIfPossibleAndNoTx() throws Exception {
    MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_JOIN_IF_POSSIBLE);
    ExecutionTemplate executionTemplate = createExecutionTemplate(config);
    Object result = executionTemplate.execute(getEmptyTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
    assertThat(TransactionCoordination.getInstance().getTransaction(), IsNull.<Object>nullValue());
  }

  @Test
  public void testActionJoinIfPossibleAndTx() throws Exception {
    TransactionCoordination.getInstance().bindTransaction(mockTransaction);
    MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_JOIN_IF_POSSIBLE);
    ExecutionTemplate executionTemplate = createExecutionTemplate(config);
    Object result = executionTemplate.execute(getEmptyTransactionCallback());
    assertThat(result, is(RETURN_VALUE));
    verify(mockTransaction, never()).commit();
    verify(mockTransaction, never()).rollback();
    assertThat((TestTransaction) TransactionCoordination.getInstance().getTransaction(), Is.is(mockTransaction));
  }

  protected ExecutionTemplate createExecutionTemplate(TransactionConfig config) {
    return TransactionalExecutionTemplate.createTransactionalExecutionTemplate(mockMuleContext, config);
  }

  protected ExecutionCallback getEmptyTransactionCallback() {
    return TransactionTemplateTestUtils.getEmptyTransactionCallback(RETURN_VALUE);
  }

  protected ExecutionCallback getRollbackTransactionCallback() {
    return TransactionTemplateTestUtils.getRollbackTransactionCallback(RETURN_VALUE);
  }

}
