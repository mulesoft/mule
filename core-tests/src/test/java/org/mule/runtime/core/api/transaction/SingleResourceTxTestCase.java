/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.transaction;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.internal.transaction.AbstractSingleResourceTransaction;
import org.mule.runtime.core.internal.transaction.AbstractTransaction;
import org.junit.Test;
import org.mule.runtime.core.internal.transaction.xa.IllegalTransactionStateException;


public class SingleResourceTxTestCase {


  @Test
  public void resourceWithWrongKey() throws TransactionException {
    AbstractTransaction transaction = new TestTransaction("test", null);
    transaction.bindResource(transaction, transaction);
    assertThat(transaction.hasResource("wrongKey"), is(false));
    assertThat(transaction.getResource("wrongKey"), is(nullValue()));
  }

  @Test
  public void correctKey() throws TransactionException {
    AbstractTransaction transaction = new TestTransaction("test", null);
    transaction.bindResource(transaction, transaction);
    assertThat(transaction.hasResource(transaction), is(true));
    assertThat(transaction.getResource(transaction), is(transaction));
  }

  @Test
  public void nullKey() {
    AbstractTransaction transaction = new TestTransaction(null, null);
    assertThat(transaction.hasResource(null), is(false));
    assertThat(transaction.getResource(null), is(nullValue()));
  }

  @Test(expected = IllegalTransactionStateException.class)
  public void bindNullKey() throws TransactionException {
    AbstractTransaction transaction = new TestTransaction("test", null);
    transaction.bindResource(null, transaction);
  }

  @Test(expected = IllegalTransactionStateException.class)
  public void bindNullResource() throws TransactionException {
    AbstractTransaction transaction = new TestTransaction("test", null);
    transaction.bindResource(transaction, null);
  }

  @Test(expected = IllegalTransactionStateException.class)
  public void bindAfterBinded() throws TransactionException {
    AbstractTransaction transaction = new TestTransaction("test", null);
    transaction.bindResource(transaction, transaction);
    transaction.bindResource(transaction, transaction);
  }

  @Test
  public void stringRepr() throws TransactionException {
    AbstractTransaction transaction = new TestTransaction("test", null);
    transaction.bindResource("hello", "world");
    assertThat(transaction.toString(), containsString("[status=STATUS_NO_TRANSACTION, key=hello]"));
    assertThat(transaction.toString(), containsString(TestTransaction.class.getName()));
  }

  @Test
  public void supports() throws TransactionException {
    AbstractTransaction transaction = new TestTransaction("test", null);
    transaction.bindResource("hello", "world");
    assertThat(transaction.supports("hello", "world"), is(true));
    assertThat(transaction.supports("hello", "no"), is(false));
    assertThat(transaction.supports("Jelou", "uorld"), is(false));
    assertThat(transaction.supports(transaction, "world"), is(false));
    assertThat(transaction.supports("hello", transaction), is(false));
  }

  private static class TestTransaction extends AbstractSingleResourceTransaction {

    protected TestTransaction(String applicationName, NotificationDispatcher notificationFirer) {
      super(applicationName, notificationFirer);
    }

    @Override
    protected void doBegin() throws TransactionException {

    }

    @Override
    protected void doCommit() throws TransactionException {

    }

    @Override
    protected void doRollback() throws TransactionException {

    }

    @Override
    protected Class getResourceType() {
      return String.class;
    }

    @Override
    protected Class getKeyType() {
      return String.class;
    }
  }
}
