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
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.transactional.connection.TestLocalTransactionalConnection;

import java.util.Collection;
import java.util.Iterator;

import org.hamcrest.MatcherAssert;
import org.junit.Test;

public class TransactionalOperationTestCase extends AbstractExtensionFunctionalTestCase {

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
    MessagingException exception = flowRunner("localTxDoesntSupportMultipleResources").runExpectingException();
    Error error = exception.getEvent().getError().get();
    MatcherAssert.assertThat(error.getDescription(),
                             is(containsString("the current transaction doesn't support it and could not be bound")));
    MatcherAssert.assertThat(error.getCause(), is(instanceOf(TransactionException.class)));
  }

  @Test
  public void localTxSupportsMultipleOperationsFromSameResource() throws Exception {
    flowRunner("localTxSupportsMultipleOperationsFromSameResource").run();
  }

  @Test
  public void pagedOperationInTx() throws Exception {
    Event event = flowRunner("pagedOperationInTx").run();
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
    Event event = flowRunner("pagedOperationWithoutTx").run();
    Collection<Integer> accumulator = (Collection<Integer>) event.getVariables().get("accumulator").getValue();
    assertThat(accumulator, is(notNullValue()));
    assertThat(accumulator, hasSize(2));

    Iterator<Integer> it = accumulator.iterator();
    Integer id1 = it.next();
    Integer id2 = it.next();

    assertThat(id1, not(equalTo(id2)));
  }
}
