/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.transaction.xa;

import static jakarta.transaction.Status.STATUS_ACTIVE;
import static jakarta.transaction.Status.STATUS_COMMITTED;
import static jakarta.transaction.Status.STATUS_COMMITTING;
import static jakarta.transaction.Status.STATUS_MARKED_ROLLBACK;
import static jakarta.transaction.Status.STATUS_NO_TRANSACTION;
import static jakarta.transaction.Status.STATUS_PREPARED;
import static jakarta.transaction.Status.STATUS_PREPARING;
import static jakarta.transaction.Status.STATUS_ROLLEDBACK;
import static jakarta.transaction.Status.STATUS_ROLLING_BACK;
import static jakarta.transaction.Status.STATUS_UNKNOWN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.mule.runtime.core.internal.transaction.xa.AbstractTransactionContext;

import org.junit.Test;

public class AbstractTransactionContextTestCase {

  @Test
  public void stringRepresentation() {
    TxContext txContext = new TxContext();
    txContext.setStatus(STATUS_ACTIVE);
    assertThat(txContext.toString(), containsString("[active, readonly]"));
    txContext.setStatus(STATUS_MARKED_ROLLBACK);
    assertThat(txContext.toString(), containsString("[marked rollback, readonly]"));
    txContext.setStatus(STATUS_PREPARED);
    assertThat(txContext.toString(), containsString("[prepared, readonly]"));
    txContext.setStatus(STATUS_COMMITTED);
    assertThat(txContext.toString(), containsString("[committed, readonly]"));
    txContext.setStatus(STATUS_ROLLEDBACK);
    assertThat(txContext.toString(), containsString("[rolled back, readonly]"));
    txContext.setStatus(STATUS_NO_TRANSACTION);
    assertThat(txContext.toString(), containsString("[no transaction, readonly]"));
    txContext.setStatus(STATUS_COMMITTING);
    assertThat(txContext.toString(), containsString("[committing, readonly]"));
    txContext.setStatus(STATUS_ROLLING_BACK);
    assertThat(txContext.toString(), containsString("[rolling back, readonly]"));
    txContext.setStatus(STATUS_UNKNOWN);
    assertThat(txContext.toString(), containsString("[unknown, readonly]"));
    txContext.setStatus(120);
    assertThat(txContext.toString(), containsString("[undefined status, readonly]"));
    txContext.setStatus(STATUS_PREPARING);
    assertThat(txContext.toString(), containsString("[preparing, readonly]"));
    txContext.notifyFinish();
    assertThat(txContext.toString(), containsString("[preparing, readonly, finished]"));
  }


  private static final class TxContext extends AbstractTransactionContext {

    @Override
    public void doCommit() throws ResourceManagerException {

    }

    @Override
    public void doRollback() throws ResourceManagerException {

    }
  }
}
