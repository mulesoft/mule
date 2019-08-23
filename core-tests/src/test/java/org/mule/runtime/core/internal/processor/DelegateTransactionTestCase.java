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
import static org.mule.tck.util.MuleContextUtils.mockMuleContext;

import org.mule.runtime.core.api.SingleResourceTransactionFactoryManager;
import org.mule.tck.junit4.AbstractMuleTestCase;

import javax.transaction.TransactionManager;
import org.junit.Before;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.core.privileged.registry.RegistrationException;

import org.junit.Test;
import org.mule.tck.util.MuleContextUtils;

public class DelegateTransactionTestCase extends AbstractMuleTestCase {

  private static final int DEFAULT_TX_TIMEOUT = 20;

  private NotificationDispatcher notificationDispatcher;

  @Before
  public void detUp() throws RegistrationException {
    notificationDispatcher = MuleContextUtils.getNotificationDispatcher(mockMuleContext());
  }

  @Test
  public void defaultTxTimeout() {
    DelegateTransaction delegateTransaction = new DelegateTransaction("appName", notificationDispatcher,
                                                                      new SingleResourceTransactionFactoryManager(),
                                                                      mock(TransactionManager.class), DEFAULT_TX_TIMEOUT);
    assertThat(delegateTransaction.getTimeout(), is(DEFAULT_TX_TIMEOUT));
  }

  @Test
  public void changeTxTimeout() {
    DelegateTransaction delegateTransaction = new DelegateTransaction("appName", notificationDispatcher,
                                                                      new SingleResourceTransactionFactoryManager(),
                                                                      mock(TransactionManager.class), DEFAULT_TX_TIMEOUT);
    int newTimeout = 10;
    delegateTransaction.setTimeout(newTimeout);
    assertThat(delegateTransaction.getTimeout(), is(newTimeout));
  }
}
