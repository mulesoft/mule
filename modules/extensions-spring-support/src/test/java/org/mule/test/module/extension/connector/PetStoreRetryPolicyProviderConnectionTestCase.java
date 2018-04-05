/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.connector;

import static java.util.stream.Collectors.toSet;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Every.everyItem;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mule.functional.api.flow.TransactionConfigEnum.ACTION_ALWAYS_BEGIN;
import static org.mule.tck.SimpleUnitTestSupportSchedulerService.UNIT_TEST_THREAD_GROUP;
import static org.mule.test.petstore.extension.PetStoreOperationsWithFailures.getConnectionThreads;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.tck.testmodels.mule.TestTransactionFactory;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class PetStoreRetryPolicyProviderConnectionTestCase extends AbstractExtensionFunctionalTestCase {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  private Transaction transaction;

  public PetStoreRetryPolicyProviderConnectionTestCase() {}

  @Override
  protected String getConfigFile() {
    return "petstore-retry-policy.xml";
  }

  @Override
  protected boolean isDisposeContextPerClass() {
    return true;
  }

  @After
  public void after() throws TransactionException {
    if (transaction != null) {
      TransactionCoordination.getInstance().unbindTransaction(transaction);
    }
  }

  @Test
  public void retryPolicyExhaustedDueToInvalidConnectionExecutingOperation() throws Exception {
    exception.expectCause(is(instanceOf(ConnectionException.class)));
    runFlow("fail-operation-with-connection-exception");

    assertThat(getConnectionThreads(), hasSize(3));
    assertThat(getConnectionThreads().stream().map(t -> t.getThreadGroup()).collect(toSet()),
               everyItem(sameInstance(UNIT_TEST_THREAD_GROUP)));
  }

  @Test
  public void retryPolicyExhaustedDueToInvalidConnectionAtValidateTime() throws Exception {
    exception.expectCause(is(instanceOf(ConnectionException.class)));
    runFlow("fail-connection-validation");

    assertThat(getConnectionThreads(), hasSize(3));
    assertThat(getConnectionThreads().stream().map(t -> t.getThreadGroup()).collect(toSet()),
               everyItem(sameInstance(UNIT_TEST_THREAD_GROUP)));
  }

  @Test
  public void retryPolicyExhaustedDueToInvalidConnectionExecutingOperationTx() throws Exception {
    exception.expectCause(is(instanceOf(ConnectionException.class)));
    transaction = createTransactionMock();
    flowRunner("fail-operation-with-connection-exception")
        .transactionally(ACTION_ALWAYS_BEGIN, new TestTransactionFactory(transaction)).run();

    assertThat(getConnectionThreads(), hasSize(3));
    assertThat(getConnectionThreads().stream().map(t -> t.getThreadGroup()).collect(toSet()),
               everyItem(sameInstance(UNIT_TEST_THREAD_GROUP)));
  }

  @Test
  public void retryPolicyExhaustedDueToInvalidConnectionAtValidateTimeTx() throws Exception {
    exception.expectCause(is(instanceOf(ConnectionException.class)));
    transaction = createTransactionMock();
    flowRunner("fail-connection-validation")
        .transactionally(ACTION_ALWAYS_BEGIN, new TestTransactionFactory(transaction)).run();

    assertThat(getConnectionThreads(), hasSize(3));
    assertThat(getConnectionThreads().stream().map(t -> t.getThreadGroup()).collect(toSet()),
               everyItem(sameInstance(UNIT_TEST_THREAD_GROUP)));
  }

  private Transaction createTransactionMock() throws TransactionException {
    Transaction transaction = mock(Transaction.class);
    doAnswer((invocationOnMock -> {
      TransactionCoordination.getInstance().bindTransaction(transaction);
      return null;
    })).when(transaction).begin();
    return transaction;
  }

  @Test
  public void retryPolicyNotExecutedDueToNotConnectionExceptionWithException() throws Exception {
    exception.expectCause(is(instanceOf(Throwable.class)));
    runFlow("fail-operation-with-not-handled-exception");
  }

  @Test
  public void retryPolicyNotExecutedDueToNotConnectionExceptionWithThrowable() throws Throwable {
    exception.expectCause(is(instanceOf(Throwable.class)));
    runFlow("fail-operation-with-not-handled-throwable");
  }
}
