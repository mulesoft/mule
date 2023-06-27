/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.transaction;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.util.func.CheckedSupplier;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.runner.RunnerDelegateTo;
import org.mule.test.transactional.SdkTransactionalSource;
import org.mule.test.transactional.TransactionalSource;
import org.mule.test.transactional.connection.MessageStorage;
import org.mule.test.transactional.connection.SdkTestTransactionalConnection;
import org.mule.test.transactional.connection.TestTransactionalConnection;

import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

import javax.transaction.TransactionManager;

@RunnerDelegateTo(Parameterized.class)
public class TransactionalSourceTestCase extends AbstractExtensionFunctionalTestCase {

  private boolean isSdkApi;
  private String configFile;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"Using Extensions API", false, "source-transaction-config.xml"},
        {"Using SDK API", true, "sdk-source-transaction-config.xml"}
    });
  }

  public TransactionalSourceTestCase(String parametrizationName, boolean isSdkApi, String configFile) {
    this.isSdkApi = isSdkApi;
    this.configFile = configFile;
  }

  @Override
  protected String getConfigFile() {
    return configFile;
  }

  @Before
  public void setUp() throws Exception {
    MessageStorage.clean();
    TransactionalSource.isSuccess = null;
    SdkTransactionalSource.isSuccess = null;
    muleContext.setTransactionManager(mock(TransactionManager.class));
  }

  @After
  public void tearDown() {
    MessageStorage.clean();
    TransactionalSource.isSuccess = null;
    SdkTransactionalSource.isSuccess = null;
  }

  @Test
  public void sourceStartsALocalTxAndGetsCommitted() throws Exception {
    startFlow("sourceStartsALocalTxAndGetsCommitted");
    validate(() -> !MessageStorage.messages.isEmpty());

    validateFlow(true);
    validateCommittedTransaction(MessageStorage.messages.poll());
  }

  @Test
  public void sourceStartsALocalTxAndGetsRollBacked() throws Exception {
    startFlow("sourceStartsALocalTxAndGetsRollBacked");
    validate(() -> !MessageStorage.messages.isEmpty());

    validateFlow(false);
    validateRolledBackedTransaction(MessageStorage.messages.poll());
  }

  @Test
  public void sourceStartsALocalTxAndOperationsCanJointIt() throws Exception {
    startFlow("sourceStartsALocalTxAndOperationsCanJointIt");
    validate(() -> MessageStorage.messages.size() == 2);

    validateFlow(true);
    validateCommittedTransaction(MessageStorage.messages.peek());
  }

  @Test
  public void sourceStartsALocalTxAndOperationsWithDifferentConnectionCanTJoinIt() throws Exception {
    startFlow("sourceStartsALocalTxAndOperationsWithDifferentConnectionCanTJoinIt");
    validate(() -> MessageStorage.exception != null);
    assertThat(MessageStorage.exception, is(instanceOf(TransactionException.class)));

    validateFlow(false);
    validateRolledBackedTransaction(MessageStorage.messages.poll());
  }

  @Test
  public void nonTxSourceDoesntBeginTx() throws Exception {
    startFlow("nonTxSourceDoesntBeginTx");
    validate(() -> !MessageStorage.messages.isEmpty());

    validateFlow(true);
    validateNonTxConnection(MessageStorage.messages.poll());
  }

  @Test
  public void nonTxSourceWithNonTxOperation() throws Exception {
    startFlow("nonTxSourceWithNonTxOperation");
    validate(() -> !MessageStorage.messages.isEmpty());

    validateFlow(true);
    validateNonTxConnection(MessageStorage.messages.poll());
  }

  @Test
  public void nonTxSourceWithTxInside() throws Exception {
    startFlow("nonTxSourceWithTxInside");
    validate(() -> !MessageStorage.messages.isEmpty());

    validateFlow(true);
    validateNonTxConnection(MessageStorage.messages.poll());
  }

  private void startFlow(String flowName) throws Exception {
    ((Flow) getFlowConstruct(flowName)).start();
  }

  private void validate(CheckedSupplier<Boolean> validation) {
    new PollingProber(10000, 100).check(new JUnitLambdaProbe(validation));
  }

  private void validateCommittedTransaction(Object connection) {
    assertTransactionConnectionState(connection, true, true, false);
  }

  private void validateRolledBackedTransaction(Object connection) {
    assertTransactionConnectionState(connection, true, false, true);
  }

  private void validateNonTxConnection(Object connection) {
    assertTransactionConnectionState(connection, false, false, false);
  }

  private void validateFlow(boolean succeeded) {
    if (isSdkApi) {
      validate(() -> SdkTransactionalSource.isSuccess != null);
      assertThat(SdkTransactionalSource.isSuccess, is(succeeded));
    } else {
      validate(() -> TransactionalSource.isSuccess != null);
      assertThat(TransactionalSource.isSuccess, is(succeeded));
    }
  }

  private void assertTransactionConnectionState(Object connection, boolean begun, boolean committed, boolean rolledBack) {
    if (connection instanceof TestTransactionalConnection) {
      assertThat(((TestTransactionalConnection) connection).isTransactionBegun(), is(begun));
      assertThat(((TestTransactionalConnection) connection).isTransactionCommited(), is(committed));
      assertThat(((TestTransactionalConnection) connection).isTransactionRolledback(), is(rolledBack));
    } else if (connection instanceof SdkTestTransactionalConnection) {
      assertThat(((SdkTestTransactionalConnection) connection).isTransactionBegun(), is(begun));
      assertThat(((SdkTestTransactionalConnection) connection).isTransactionCommited(), is(committed));
      assertThat(((SdkTestTransactionalConnection) connection).isTransactionRolledback(), is(rolledBack));
    } else {
      throw new RuntimeException("Stored object is not a valid type of connection");
    }
  }
}
