/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.transaction;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mule.functional.junit4.matchers.ThrowableMessageMatcher.hasMessage;

import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.transaction.xa.IllegalTransactionStateException;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.transactional.connection.TestLocalTransactionalConnection;

import java.util.Collection;
import java.util.Iterator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TransactionalOperationTestCase extends AbstractExtensionFunctionalTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Override
  protected String getConfigFile() {
    return "transaction-config.xml";
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
    TestLocalTransactionalConnection connection =
        (TestLocalTransactionalConnection) flowRunner("executeTransactionless").withPayload("")
            .run().getMessage().getPayload().getValue();
    assertThat(connection.isTransactionBegun(), is(false));
    assertThat(connection.isTransactionCommited(), is(false));
    assertThat(connection.isTransactionRolledback(), is(false));
  }

  @Test
  public void localTxDoesntSupportMultipleResources() throws Exception {
    flowRunner("localTxDoesntSupportMultipleResources").runExpectingException(allOf(instanceOf(TransactionException.class),
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
  public void cantNestTransactions() throws Exception {
    expectedException.expectMessage("Non-XA transactions can't be nested.");
    expectedException.expectCause(is(instanceOf(IllegalTransactionStateException.class)));
    flowRunner("cantNestTransactions").run();
  }

  @Test
  public void operationJoinsAlreadyCreatedTx() throws Exception {
    flowRunner("operationJoinsAlreadyCreatedTx").run();
  }
}
