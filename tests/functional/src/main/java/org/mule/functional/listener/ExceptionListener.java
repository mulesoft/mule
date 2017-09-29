/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.listener;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.runtime.api.notification.ExceptionNotification;
import org.mule.runtime.api.notification.ExceptionNotificationListener;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.api.util.concurrent.Latch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Listener for exception thrown by a message source or flow.
 */
public class ExceptionListener {

  private CountDownLatch exceptionThrownLatch = new Latch();
  private int timeout = 10000;
  private List<ExceptionNotification> exceptionNotifications = new ArrayList<>();
  private AtomicInteger numberOfInvocations = new AtomicInteger();
  private List<java.util.function.Consumer<ExceptionNotification>> listeners = new ArrayList<>();

  /**
   * Constructor for creating a listener for any exception thrown within a flow or message source.
   */
  public ExceptionListener(NotificationListenerRegistry notificationListenerRegistry) {
    notificationListenerRegistry.registerListener((ExceptionNotificationListener) notification -> {
      exceptionNotifications.add(notification);
      exceptionThrownLatch.countDown();
      for (Consumer<ExceptionNotification> listener : listeners) {
        listener.accept(notification);
      }
    });
  }

  public ExceptionListener waitUntilAllNotificationsAreReceived() {
    try {
      if (!exceptionThrownLatch.await(timeout, MILLISECONDS)) {
        fail("There was no exception notification");
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  public ExceptionListener assertExpectedException(final Class<? extends Throwable> expectedExceptionType) {
    for (ExceptionNotification exceptionNotification : exceptionNotifications) {
      if (expectedExceptionType.isAssignableFrom(exceptionNotification.getException().getClass())) {
        return this;
      }
    }
    throw new AssertionError(format("Exception exception type %s was not thrown", expectedExceptionType.getName()));
  }

  /**
   * Asserts that there is at least one exception with the given cause
   *
   * @param expectedExceptionCauseType the excepted cause of a thrown exception
   * @return the exception listener
   */
  public ExceptionListener assertExpectedExceptionCausedBy(final Class<? extends Throwable> expectedExceptionCauseType) {
    for (ExceptionNotification exceptionNotification : exceptionNotifications) {
      Throwable exceptionThrownCause = exceptionNotification.getException().getCause();
      if (exceptionThrownCause != null && expectedExceptionCauseType.isAssignableFrom(exceptionThrownCause.getClass())) {
        return this;
      }
    }
    throw new AssertionError(format("Exception exception caused by type %s was not thrown",
                                    expectedExceptionCauseType.getName()));
  }

  /**
   * @param numberOfExecutionsRequired number of times that the listener must be notified before releasing the latch.
   */
  public ExceptionListener setNumberOfExecutionsRequired(int numberOfExecutionsRequired) {
    this.exceptionThrownLatch = new CountDownLatch(numberOfExecutionsRequired);
    return this;
  }

  public ExceptionListener setTimeoutInMillis(int timeout) {
    this.timeout = timeout;
    return this;
  }

  public void addListener(Consumer<ExceptionNotification> listener) {
    this.listeners.add(listener);
  }

  /**
   * Asserts that the exception handler was not invoked.
   */
  public void assertNotInvoked() {
    assertThat(this.numberOfInvocations.get(), is(0));
  }
}
