/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.transaction;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.util.func.CheckedSupplier;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.transactional.TransactionalSource;
import org.mule.test.transactional.connection.MessageStorage;
import org.mule.test.transactional.connection.TestTransactionalConnection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TransactionalSourceTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "source-transaction-config.xml";
  }

  @Before
  public void setUp() {
    MessageStorage.clean();
    TransactionalSource.isSuccess = null;
  }

  @After
  public void tearDown() {
    MessageStorage.clean();
    TransactionalSource.isSuccess = null;
  }

  @Test
  public void sourceStartsALocalTxAndGetsCommitted() throws Exception {
    startFlow("sourceStartsALocalTxAndGetsCommitted");
    validate(() -> !MessageStorage.messages.isEmpty());

    validateSuccessFlow();
    validateCommittedTransaction(MessageStorage.messages.poll());
  }

  @Test
  public void sourceStartsALocalTxAndGetsRollBacked() throws Exception {
    startFlow("sourceStartsALocalTxAndGetsRollBacked");
    validate(() -> !MessageStorage.messages.isEmpty());

    validateErrorFlow();
    validateRolledBackedTransaction(MessageStorage.messages.poll());
  }

  @Test
  public void sourceStartsALocalTxAndOperationsCanJointIt() throws Exception {
    startFlow("sourceStartsALocalTxAndOperationsCanJointIt");
    validate(() -> MessageStorage.messages.size() == 2);

    validateSuccessFlow();
    validateCommittedTransaction(MessageStorage.messages.peek());
  }

  @Test
  public void sourceStartsALocalTxAndOperationsWithDifferentConnectionCanTJoinIt() throws Exception {
    startFlow("sourceStartsALocalTxAndOperationsWithDifferentConnectionCanTJoinIt");
    validate(() -> MessageStorage.exception != null);
    assertThat(MessageStorage.exception, is(instanceOf(TransactionException.class)));

    validateErrorFlow();
    validateRolledBackedTransaction(MessageStorage.messages.poll());
  }

  @Test
  public void nonTxSourceDoesntBeginTx() throws Exception {
    startFlow("nonTxSourceDoesntBeginTx");
    validate(() -> !MessageStorage.messages.isEmpty());

    validateSuccessFlow();
    validateNonTxConnection(MessageStorage.messages.poll());
  }

  @Test
  public void nonTxSourceWithNonTxOperation() throws Exception {
    startFlow("nonTxSourceWithNonTxOperation");
    validate(() -> !MessageStorage.messages.isEmpty());

    validateSuccessFlow();
    validateNonTxConnection(MessageStorage.messages.poll());
  }

  @Test
  public void nonTxSourceWithTxInside() throws Exception {
    startFlow("nonTxSourceWithTxInside");
    validate(() -> !MessageStorage.messages.isEmpty());

    validateSuccessFlow();
    validateNonTxConnection(MessageStorage.messages.poll());
  }

  private void startFlow(String flowName) throws Exception {
    ((Flow) getFlowConstruct(flowName)).start();
  }

  private void validate(CheckedSupplier<Boolean> validation) {
    new PollingProber(10000, 100).check(new JUnitLambdaProbe(validation));
  }

  private void validateCommittedTransaction(TestTransactionalConnection connection) {
    assertThat(connection.isTransactionBegun(), is(true));
    assertThat(connection.isTransactionCommited(), is(true));
    assertThat(connection.isTransactionRolledback(), is(false));
  }

  private void validateRolledBackedTransaction(TestTransactionalConnection connection) {
    assertThat(connection.isTransactionBegun(), is(true));
    assertThat(connection.isTransactionCommited(), is(false));
    assertThat(connection.isTransactionRolledback(), is(true));
  }

  private void validateNonTxConnection(TestTransactionalConnection connection) {
    assertThat(connection.isTransactionBegun(), is(false));
    assertThat(connection.isTransactionCommited(), is(false));
    assertThat(connection.isTransactionRolledback(), is(false));
  }

  private void validateSuccessFlow() {
    validate(() -> TransactionalSource.isSuccess != null);
    assertThat(TransactionalSource.isSuccess, is(true));
  }

  private void validateErrorFlow() {
    validate(() -> TransactionalSource.isSuccess != null);
    assertThat(TransactionalSource.isSuccess, is(false));
  }
}
