/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.connector;

import static java.lang.Thread.currentThread;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Every.everyItem;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mule.functional.api.flow.TransactionConfigEnum.ACTION_ALWAYS_BEGIN;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.CONNECTIVITY_ERROR_IDENTIFIER;
import static org.mule.tck.SimpleUnitTestSupportSchedulerService.UNIT_TEST_THREAD_GROUP;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;
import static org.mule.test.petstore.extension.PetStoreOperationsWithFailures.getConnectionThreads;
import static org.mule.test.petstore.extension.PetStoreOperationsWithFailures.resetConnectionThreads;

import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.tck.testmodels.mule.TestTransactionFactory;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Set;

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

  @Before
  public void before() {
    resetConnectionThreads();
  }

  @After
  public void after() throws TransactionException {
    resetConnectionThreads();
    if (transaction != null) {
      TransactionCoordination.getInstance().unbindTransaction(transaction);
    }
  }

  @Test
  public void retryPolicyExhaustedDueToInvalidConnectionExecutingOperation() throws Exception {
    flowRunner("fail-operation-with-connection-exception")
        .runExpectingException(errorType("PETSTORE", CONNECTIVITY_ERROR_IDENTIFIER));

    assertThat(getConnectionThreads(), hasSize(3));
    assertThat(getConnectionThreads().stream().map(t -> t.getThreadGroup()).collect(toSet()),
               everyItem(sameInstance(UNIT_TEST_THREAD_GROUP)));
  }

  @Test
  public void retryPolicyExhaustedDueToInvalidConnectionAtValidateTime() throws Exception {
    flowRunner("fail-connection-validation").runExpectingException(errorType("PETSTORE", CONNECTIVITY_ERROR_IDENTIFIER));

    assertThat(getConnectionThreads(), hasSize(3));
    assertThat(getConnectionThreads().stream().map(t -> t.getThreadGroup()).collect(toSet()),
               everyItem(sameInstance(UNIT_TEST_THREAD_GROUP)));
  }

  @Test
  public void retryPolicyExhaustedDueToInvalidConnectionExecutingOperationTxFlow() throws Exception {
    transaction = createTransactionMock();
    flowRunner("fail-operation-with-connection-exception")
        .transactionally(ACTION_ALWAYS_BEGIN, new TestTransactionFactory(transaction))
        .runExpectingException(errorType("PETSTORE", CONNECTIVITY_ERROR_IDENTIFIER));

    assertThat(getConnectionThreads(), hasSize(3));
    Set<ThreadGroup> connectionThreadsSet = getConnectionThreads().stream().map(t -> t.getThreadGroup()).collect(toSet());
    assertThat("Transactional retry must not change threads", connectionThreadsSet, hasSize(1));
    assertThat(connectionThreadsSet, everyItem(sameInstance(currentThread().getThreadGroup())));
  }

  @Test
  public void retryPolicyExhaustedDueToInvalidConnectionAtValidateTimeTxFlow() throws Exception {
    transaction = createTransactionMock();
    flowRunner("fail-connection-validation").transactionally(ACTION_ALWAYS_BEGIN, new TestTransactionFactory(transaction))
        .runExpectingException(errorType("PETSTORE", CONNECTIVITY_ERROR_IDENTIFIER));

    assertThat(getConnectionThreads(), hasSize(3));
    Set<ThreadGroup> connectionThreadsSet = getConnectionThreads().stream().map(t -> t.getThreadGroup()).collect(toSet());
    assertThat("Transactional retry must not change threads", connectionThreadsSet, hasSize(1));
    assertThat(connectionThreadsSet, everyItem(sameInstance(currentThread().getThreadGroup())));
  }

  @Test
  public void retryPolicyExhaustedDueToInvalidConnectionExecutingOperationTxOperation() throws Exception {
    flowRunner("fail-operation-with-connection-exception-tx")
        .runExpectingException(errorType("PETSTORE", CONNECTIVITY_ERROR_IDENTIFIER));

    assertThat(getConnectionThreads(), hasSize(3));
    Set<ThreadGroup> connectionThreadsSet = getConnectionThreads().stream().map(t -> t.getThreadGroup()).collect(toSet());
    assertThat("Transactional retry must not change threads", connectionThreadsSet, hasSize(1));
    assertThat(connectionThreadsSet, everyItem(sameInstance(UNIT_TEST_THREAD_GROUP)));
  }

  @Test
  public void retryPolicyExhaustedDueToInvalidConnectionAtValidateTimeTxOperation() throws Exception {
    flowRunner("fail-connection-validation-tx").runExpectingException(errorType("PETSTORE", CONNECTIVITY_ERROR_IDENTIFIER));

    assertThat(getConnectionThreads(), hasSize(3));
    Set<ThreadGroup> connectionThreadsSet = getConnectionThreads().stream().map(t -> t.getThreadGroup()).collect(toSet());
    assertThat("Transactional retry must not change threads", connectionThreadsSet, hasSize(1));
    assertThat(connectionThreadsSet, everyItem(sameInstance(UNIT_TEST_THREAD_GROUP)));
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
    flowRunner("fail-operation-with-not-handled-exception").runExpectingException();
  }

  @Test
  public void retryPolicyNotExecutedDueToNotConnectionExceptionWithThrowable() throws Throwable {
    flowRunner("fail-operation-with-not-handled-throwable").runExpectingException();
  }
}
