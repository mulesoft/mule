/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.transaction;

import static org.mule.functional.junit4.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mule.test.transactional.TransactionalOperations.getPageCalls;

import static java.util.Arrays.asList;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasSize;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.transaction.TransactionStatusException;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class TransactionalOperationTestCase extends AbstractExtensionFunctionalTestCase {

  private final String configFile;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<String> configs() {
    return asList("tx/transaction-config.xml",
                  "tx/transaction-xa-config.xml");
  }

  public TransactionalOperationTestCase(String configFile) {
    this.configFile = configFile;
  }

  @Override
  protected String getConfigFile() {
    return configFile;
  }

  @Test
  public void commited() throws Exception {
    flowRunner("commitLocalTx").withPayload("").run();
  }

  @Test
  public void rolledBack() throws Exception {
    flowRunner("rollbackLocalTx").withPayload("").run();
  }

  @Test
  public void executeTransactionless() throws Exception {
    final Map<String, Boolean> result = (Map<String, Boolean>) flowRunner("executeTransactionless").withPayload("")
        .run().getMessage().getPayload().getValue();

    assertThat(result.get("transactionBegun"), is(false));
    assertThat(result.get("transactionCommited"), is(false));
    assertThat(result.get("transactionRolledback"), is(false));
  }

  @Test
  public void localTxDoesntSupportMultipleResources() throws Exception {
    flowRunner("localTxDoesntSupportMultipleResources")
        .runExpectingException(allOf(instanceOf(TransactionException.class),
                                     hasMessage(containsString("the current transaction doesn't support it and could not be bound"))));
  }

  @Test
  public void localTxSupportsMultipleOperationsFromSameResource() throws Exception {
    flowRunner("localTxSupportsMultipleOperationsFromSameResource").run();
  }

  @Test
  public void pagedOperationInTx() throws Exception {
    CoreEvent event = flowRunner("pagedOperationInTx").run();
    Collection<Integer> accumulator = (Collection<Integer>) event.getVariables().get("accumulator").getValue();
    assertThat(accumulator, is(notNullValue()));
    assertThat(accumulator, hasSize(2));

    Iterator<Integer> it = accumulator.iterator();
    Integer id1 = it.next();
    Integer id2 = it.next();

    assertThat(id1, equalTo(id2));
  }

  @Test
  public void pagedOperationInTxAlwaysUsesSameConnection() throws Exception {
    CoreEvent event = flowRunner("pagedOperationInTxAlwaysUsesSameConnection").run();
    List<?> connections = (List) event.getVariables().get("connections").getValue();
    assertThat(connections, is(notNullValue()));
    assertThat(connections, hasSize(2));

    Object connection = connections.get(0);
    assertThat(connections.stream().allMatch(c -> c == connection), is(true));
  }

  @Test
  public void pagedOperationWithoutTx() throws Exception {
    CoreEvent event = flowRunner("pagedOperationWithoutTx").run();
    Collection<Integer> accumulator = (Collection<Integer>) event.getVariables().get("accumulator").getValue();
    assertThat(accumulator, is(notNullValue()));
    assertThat(accumulator, hasSize(2));

    Iterator<Integer> it = accumulator.iterator();
    Integer id1 = it.next();
    Integer id2 = it.next();

    assertThat(id1, not(equalTo(id2)));
  }

  @Test
  public void doNotReconnectPagedOperationInTx() throws Exception {
    resetCounters();
    final var failingPagedOperationInTx = flowRunner("failingPagedOperationInTx")
        .withVariable("failOn", 1);
    var thrown = assertThrows(Exception.class, () -> failingPagedOperationInTx.run());
    assertThat(thrown.getCause(), instanceOf(ConnectionException.class));
    assertThat(thrown.getMessage(), containsString("Failed to retrieve Page"));
  }

  @Test
  public void doNotReconnectStickyPagedOperationInTx() throws Exception {
    resetCounters();
    final var stickyFailingPagedOperationInTx = flowRunner("stickyFailingPagedOperationInTx")
        .withVariable("failOn", 1);
    var thrown = assertThrows(Exception.class, () -> stickyFailingPagedOperationInTx.run());
    assertThat(thrown.getCause(), instanceOf(ConnectionException.class));
    assertThat(thrown.getMessage(), containsString("Failed to retrieve Page"));
  }

  @Test
  @Ignore("MULE-19198")
  public void doNotReconnectPagedOperationInTxWhenConnectionExceptionOnSecondPage() throws Exception {
    resetCounters();
    final var failingPagedOperationInTx = flowRunner("failingPagedOperationInTx")
        .withVariable("failOn", 2);
    var thrown = assertThrows(Exception.class, () -> failingPagedOperationInTx.run());
    assertThat(thrown.getCause(), instanceOf(ConnectionException.class));
    assertThat(thrown.getMessage(), containsString("Failed to retrieve Page"));
  }

  @Test
  @Ignore("MULE-19198")
  public void doNotReconnectStickyPagedOperationInTxWhenConnectionExceptionOnSecondPage() throws Exception {
    resetCounters();
    final var stickyFailingPagedOperationInTx = flowRunner("stickyFailingPagedOperationInTx")
        .withVariable("failOn", 2);
    var thrown = assertThrows(Exception.class, () -> stickyFailingPagedOperationInTx.run());
    assertThat(thrown.getCause(), instanceOf(ConnectionException.class));
    assertThat(thrown.getMessage(), containsString("Failed to retrieve Page"));
  }

  @Test
  public void doReconnectPagedOperationWithoutTx() throws Exception {
    resetCounters();
    CoreEvent event = flowRunner("failingPagedOperationWithoutTx").withVariable("failOn", 1).run();
    Collection<Integer> accumulator = (Collection<Integer>) event.getVariables().get("accumulator").getValue();
    assertThat(accumulator, is(notNullValue()));
    assertThat(accumulator, hasSize(2));
  }

  @Test
  public void doReconnectStickyPagedOperationWithoutTx() throws Exception {
    resetCounters();
    CoreEvent event = flowRunner("stickyFailingPagedOperationWithoutTx").withVariable("failOn", 1).run();
    Collection<Integer> accumulator = (Collection<Integer>) event.getVariables().get("accumulator").getValue();
    assertThat(accumulator, is(notNullValue()));
    assertThat(accumulator, hasSize(2));
  }

  @Test
  public void doReconnectPagedOperationWithoutTxWhenConnectionExceptionOnSecondPage() throws Exception {
    resetCounters();
    CoreEvent event = flowRunner("failingPagedOperationWithoutTx").withVariable("failOn", 2).run();
    Collection<Integer> accumulator = (Collection<Integer>) event.getVariables().get("accumulator").getValue();
    assertThat(accumulator, is(notNullValue()));
    assertThat(accumulator, hasSize(2));
  }

  @Test
  @Ignore("MULE-19198")
  public void doNotReconnectStickyPagedOperationWithoutTxWhenConnectionExceptionOnSecondPage() throws Exception {
    resetCounters();
    final var stickyFailingPagedOperationInTx = flowRunner("stickyFailingPagedOperationInTx")
        .withVariable("failOn", 2);
    var thrown = assertThrows(Exception.class, () -> stickyFailingPagedOperationInTx.run());
    assertThat(thrown.getCause(), instanceOf(ConnectionException.class));
    assertThat(thrown.getMessage(), containsString("Failed to retrieve Page"));
  }

  @Test
  public void cantNestTransactions() throws Exception {
    final var cantNestTransactions = flowRunner("cantNestTransactions");
    var thrown = assertThrows(Exception.class, () -> cantNestTransactions.run());
    assertThat(thrown.getCause(), instanceOf(TransactionStatusException.class));
    assertThat(thrown.getMessage(), containsString("Non-XA transactions can't be nested."));
  }

  @Test
  public void operationJoinsAlreadyCreatedTx() throws Exception {
    flowRunner("operationJoinsAlreadyCreatedTx").run();
  }

  @Test
  public void doNotRetryOnTxReconnection() throws Exception {
    final var doNotRetryOnTxReconnection = flowRunner("doNotRetryOnTxReconnection");
    var thrown = assertThrows(Exception.class, () -> doNotRetryOnTxReconnection.run());
    assertThat(thrown.getCause(), instanceOf(ConnectionException.class));
    assertThat(thrown.getMessage(), is("1"));
  }

  private void resetCounters() {
    getPageCalls = 0;
    org.mule.test.transactionalxa.TransactionalOperations.getPageCalls = 0;
  }
}
