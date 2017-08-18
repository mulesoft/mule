/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.slf4j.Logger;

@SmallTest
public class OneTimeWarningTestCase extends AbstractMuleTestCase {

  private static final int TIMEOUT = 5;

  @Test
  public void warn() throws Exception {
    String message = "Hello World!";
    Logger logger = mock(Logger.class);
    final int competitors = 10;
    final CountDownLatch warnLatch = new CountDownLatch(competitors);
    final CountDownLatch completionLatch = new CountDownLatch(competitors);
    final OneTimeWarning warning = new OneTimeWarning(logger, message);
    final AtomicReference<Throwable> exception = new AtomicReference<>(null);

    for (int i = 0; i < competitors; i++) {
      new Thread() {

        @Override
        public void run() {
          warnLatch.countDown();
          try {
            warnLatch.await(TIMEOUT, SECONDS);
          } catch (Throwable t) {
            exception.set(t);
          }
          warning.warn();
          completionLatch.countDown();
        }
      }.start();
    }

    completionLatch.await(TIMEOUT, SECONDS);
    assertThat(exception.get(), is(nullValue()));
    verify(logger).warn(message);
  }
}
