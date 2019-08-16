/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.mule.runtime.core.api.SingleResourceTransactionFactoryManager;
import org.mule.runtime.core.internal.context.notification.DefaultNotificationDispatcher;
import org.mule.tck.junit4.AbstractMuleTestCase;

import javax.transaction.TransactionManager;

import org.junit.Test;

public class DelegateTransactionTestCase extends AbstractMuleTestCase {

  private static final int DEFAULT_TX_TIMEOUT = 20;

  @Test
  public void defaultTxTimeout() {
    DelegateTransaction delegateTransaction = new DelegateTransaction("appName", new DefaultNotificationDispatcher(),
                                                                      new SingleResourceTransactionFactoryManager(),
                                                                      mock(TransactionManager.class), DEFAULT_TX_TIMEOUT);
    assertThat(delegateTransaction.getTimeout(), is(DEFAULT_TX_TIMEOUT));
  }

  @Test
  public void changeTxTimeout() {
    DelegateTransaction delegateTransaction = new DelegateTransaction("appName", new DefaultNotificationDispatcher(),
                                                                      new SingleResourceTransactionFactoryManager(),
                                                                      mock(TransactionManager.class), DEFAULT_TX_TIMEOUT);
    int newTimeout = 10;
    delegateTransaction.setTimeout(newTimeout);
    assertThat(delegateTransaction.getTimeout(), is(newTimeout));
  }
}
