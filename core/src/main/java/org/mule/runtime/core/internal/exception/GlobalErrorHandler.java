/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static org.mule.runtime.core.privileged.exception.TemplateOnErrorHandler.reuseGlobalErrorHandler;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.exception.TemplateOnErrorHandler;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;

public class GlobalErrorHandler extends ErrorHandler {

  private final AtomicBoolean initialised = new AtomicBoolean(false);
  private final AtomicInteger started = new AtomicInteger(0);

  private Map<MessageProcessorChain, Consumer<Exception>> routers = new HashMap<>();

  @Override
  public Publisher<CoreEvent> apply(Exception exception) {
    throw new IllegalStateException("GlobalErrorHandlers should be used only as template for local ErrorHandlers");
  }

  public Consumer<Exception> routerForChain(MessageProcessorChain chain, Supplier<Consumer<Exception>> errorRouterSupplier) {
    if (!reuseGlobalErrorHandler()) {
      return errorRouterSupplier.get();
    }
    if (!routers.containsKey(chain)) {
      routers.put(chain, errorRouterSupplier.get());
    }
    return routers.get(chain);
  }

  @Override
  public void initialise() throws InitialisationException {
    if (!reuseGlobalErrorHandler()) {
      super.initialise();
      return;
    }

    if (!initialised.getAndSet(true)) {
      super.initialise();
    }
  }

  @Override
  public void start() throws MuleException {
    if (!reuseGlobalErrorHandler()) {
      super.start();
      return;
    }

    if (started.getAndIncrement() == 0) {
      super.start();
    }
  }

  @Override
  public void stop() throws MuleException {
    if (!reuseGlobalErrorHandler()) {
      super.stop();
      return;
    }

    if (started.decrementAndGet() == 0) {
      super.stop();
    }
  }

  @Override
  public void dispose() {
    if (!reuseGlobalErrorHandler()) {
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

  public void clearRouterForChain(MessageProcessorChain chain) {
    if (reuseGlobalErrorHandler()) {
      routers.remove(chain);
    }
  }
}
