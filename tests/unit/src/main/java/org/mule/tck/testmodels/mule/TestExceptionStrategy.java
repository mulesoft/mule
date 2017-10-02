/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.mule;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.exception.RollbackSourceCallback;
import org.mule.runtime.core.api.exception.SystemExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Provides a exception strategy for testing purposes.
 */
public class TestExceptionStrategy implements FlowExceptionHandler, SystemExceptionHandler {

  /**
   * logger used by this class
   */
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * This is the lock that protect both the storage of {@link #callback} and modifications of {@link #unhandled}.
   */
  private Object callbackLock = new Object();

  // @GuardedBy("callbackLock")
  private ExceptionCallback callback;
  // @GuardedBy("callbackLock")
  private List<Exception> unhandled = new LinkedList<>();

  private volatile String testProperty;

  public TestExceptionStrategy() {}

  public String getTestProperty() {
    return testProperty;
  }

  public void setTestProperty(String testProperty) {
    this.testProperty = testProperty;
  }

  public CoreEvent handleException(Exception exception, CoreEvent event, RollbackSourceCallback rollbackMethod) {
    ExceptionCallback callback = null;
    synchronized (callbackLock) {
      if (this.callback != null) {
        callback = this.callback;
      } else {
        unhandled.add(exception);
      }
    }
    // It is important that the call to the callback is done outside
    // synchronization since we don't control that code and
    // we could have liveness problems.
    logger.info("Handling exception: " + exception.getClass().getName());
    if (callback != null) {
      logger.info("Exception caught on TestExceptionStrategy and was sent to callback.", exception);
      callback.onException(exception);
    } else {
      logger.info("Exception caught on TestExceptionStrategy but there was no callback set.", exception);
    }
    return event;
  }

  @Override
  public CoreEvent handleException(Exception exception, CoreEvent event) {
    return handleException(exception, event, null);
  }

  @Override
  public void handleException(Exception exception, RollbackSourceCallback rollbackMethod) {
    handleException(exception, null, rollbackMethod);
  }

  @Override
  public void handleException(Exception exception) {
    handleException(exception, null, null);
  }

  public interface ExceptionCallback {

    void onException(Throwable t);
  }

  public void setExceptionCallback(ExceptionCallback exceptionCallback) {
    synchronized (callbackLock) {
      this.callback = exceptionCallback;
    }
    processUnhandled();
  }

  protected void processUnhandled() {
    List<Exception> unhandledCopies = null;
    ExceptionCallback callback = null;
    synchronized (callbackLock) {
      if (this.callback != null) {
        callback = this.callback;
        unhandledCopies = new ArrayList<>(unhandled);
        unhandled.clear();
      }
    }
    // It is important that the call to the callback is done outside
    // synchronization since we don't control that code and
    // we could have liveness problems.
    if (callback != null && unhandledCopies != null) {
      for (Exception exception : unhandledCopies) {
        logger.info("Handling exception after setting the callback.", exception);
        callback.onException(exception);
      }
    }
  }
}
