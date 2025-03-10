/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor;

import static org.mule.tck.util.MuleContextUtils.mockMuleContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.core.internal.transaction.DelegateTransaction;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.util.MuleContextUtils;

import org.junit.Before;
import org.junit.Test;

public class DelegateTransactionTestCase extends AbstractMuleTestCase {

  private static final int DEFAULT_TX_TIMEOUT = 30000;

  private final String applicationName = "appName";
  private NotificationDispatcher notificationDispatcher;

  @Before
  public void detUp() throws RegistrationException {
    notificationDispatcher = MuleContextUtils.getNotificationDispatcher(mockMuleContext());
  }

  @Test
  public void defaultTxTimeout() {
    DelegateTransaction delegateTransaction = new DelegateTransaction(applicationName, notificationDispatcher);
    assertThat(delegateTransaction.getTimeout(), is(DEFAULT_TX_TIMEOUT));
  }

  @Test
  public void changeTxTimeout() {
    DelegateTransaction delegateTransaction = new DelegateTransaction(applicationName, notificationDispatcher);
    int newTimeout = 10;
    delegateTransaction.setTimeout(newTimeout);
    assertThat(delegateTransaction.getTimeout(), is(newTimeout));
  }
}
