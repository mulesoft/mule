/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.context.notification.ExceptionNotificationListener;
import org.mule.runtime.core.api.context.notification.TransactionNotificationListener;
import org.mule.runtime.core.context.notification.TransactionNotification;
import org.mule.runtime.core.util.concurrent.Latch;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Test;

public class ExceptionStrategyExceptionPatternTestCase extends FunctionalTestCase {

  public static final String PAYLOAD = "some text";
  public static final int TIMEOUT = 5000;
  private Latch exceptionLatch = new Latch();
  private Latch commitLatch = new Latch();
  private Latch rollbackLatch = new Latch();
  private AtomicReference<Exception> exceptionHolder = new AtomicReference<>();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/exception-strategy-exception-pattern-flow.xml";
  }

  @Before
  public void setUp() throws Exception {
    final ExceptionNotificationListener exceptionListener = notification -> exceptionLatch.release();
    muleContext.registerListener(exceptionListener);
    FunctionalTestComponent failingFlow = getFunctionalTestComponent("failingFlow");
    failingFlow.setEventCallback((context, component, muleContext) -> {
      throw exceptionHolder.get();
    });
    final TransactionNotificationListener txListener = notification -> {
      if (notification.getAction() == TransactionNotification.TRANSACTION_COMMITTED) {
        commitLatch.release();
      } else if (notification.getAction() == TransactionNotification.TRANSACTION_ROLLEDBACK) {
        rollbackLatch.release();
      }
    };
    muleContext.registerListener(txListener);
  }

  @Test
  public void testThrowExceptionAndCommit() throws Exception {

    MuleClient client = muleContext.getClient();
    exceptionHolder.set(new IOException());
    client.dispatch("jms://in", PAYLOAD, null);
    if (!exceptionLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("exception should be thrown");
    }
    MuleMessage muleMessage = client.request("jms://out", TIMEOUT).getRight().get();
    assertThat(muleMessage, IsNull.notNullValue());
  }

  @Test
  public void testThrowExceptionAndRollback() throws Exception {

    MuleClient client = muleContext.getClient();
    exceptionHolder.set(new IllegalArgumentException());
    client.dispatch("jms://in", PAYLOAD, null);
    if (!exceptionLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)) {
      fail("exception should be thrown");
    }
    assertThat(client.request("jms://out", TIMEOUT).getRight().isPresent(), is(false));
  }
}
