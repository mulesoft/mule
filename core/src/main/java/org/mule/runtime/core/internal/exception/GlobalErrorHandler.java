/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static org.mule.runtime.core.privileged.exception.TemplateOnErrorHandler.REUSE_GLOBAL_ERROR_HANDLER;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.exception.TemplateOnErrorHandler;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.reactivestreams.Publisher;

public class GlobalErrorHandler extends ErrorHandler {

  private final AtomicBoolean initialised = new AtomicBoolean(false);
  private final AtomicInteger started = new AtomicInteger(0);

  @Override
  public Publisher<CoreEvent> apply(Exception exception) {
    throw new IllegalStateException("GlobalErrorHandlers should be used only as template for local ErrorHandlers");
  }

  @Override
  public void initialise() throws InitialisationException {
    if (!REUSE_GLOBAL_ERROR_HANDLER) {
      super.initialise();
      return;
    }

    if (!initialised.getAndSet(true)) {
      super.initialise();
    }
  }

  @Override
  public void start() throws MuleException {
    if (!REUSE_GLOBAL_ERROR_HANDLER) {
      super.start();
      return;
    }

    if (started.getAndIncrement() == 0) {
      super.start();
    }
  }

  @Override
  public void stop() throws MuleException {
    if (!REUSE_GLOBAL_ERROR_HANDLER) {
      super.stop();
      return;
    }

    if (started.decrementAndGet() == 0) {
      super.stop();
    }
  }

  @Override
  public void dispose() {
    if (!REUSE_GLOBAL_ERROR_HANDLER) {
      super.dispose();
      return;
    }

    if (started.get() == 0 && initialised.getAndSet(false)) {
      super.dispose();
    }
  }

  public void setFromGlobalErrorHandler() {
    this.getExceptionListeners().stream()
        .filter(TemplateOnErrorHandler.class::isInstance)
        .forEach(exceptionListener -> ((TemplateOnErrorHandler) exceptionListener).setFromGlobalErrorHandler(true));
  }

  public ErrorHandler createLocalErrorHandler(ComponentLocation flowLocation) {
    ErrorHandler local = new ErrorHandler();
    local.setName(name);
    local.setExceptionListeners(getExceptionListeners());
    local.setExceptionListenersLocation(flowLocation);
    return local;
  }
}
