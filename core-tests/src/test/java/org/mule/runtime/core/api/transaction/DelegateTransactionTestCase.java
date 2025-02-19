/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.transaction;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.internal.transaction.DelegateTransaction;
import org.junit.Test;

public class DelegateTransactionTestCase {

  @Test
  public void nullTx() throws TransactionException {
    DelegateTransaction delegateTransaction = new DelegateTransaction("app", mock(NotificationDispatcher.class), null);
    assertThat(delegateTransaction.isBegun(), is(false));
    assertThat(delegateTransaction.isRollbackOnly(), is(false));
    assertThat(delegateTransaction.isRolledBack(), is(false));
    delegateTransaction.resume();
    assertThat(delegateTransaction.suspend(), is(nullValue()));
    delegateTransaction.commit();
    delegateTransaction.rollback();
    delegateTransaction.begin();
    delegateTransaction.setRollbackOnly();
    assertThat(delegateTransaction.isBegun(), is(false));
    assertThat(delegateTransaction.isRollbackOnly(), is(false));
    assertThat(delegateTransaction.isRolledBack(), is(false));
    delegateTransaction.setTimeout(20);
    assertThat(delegateTransaction.getTimeout(), is(20));
  }

}
