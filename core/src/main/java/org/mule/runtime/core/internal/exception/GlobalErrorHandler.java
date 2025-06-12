/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.Component;
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
import org.slf4j.Logger;

public class GlobalErrorHandler extends ErrorHandler {

  private static final Logger LOGGER = getLogger(GlobalErrorHandler.class);

  private final AtomicBoolean initialised = new AtomicBoolean(false);
  private final AtomicInteger started = new AtomicInteger(0);

  private Map<Component, Consumer<Exception>> routers = new HashMap<>();

  @Override
  public Publisher<CoreEvent> apply(Exception exception) {
    throw new IllegalStateException("GlobalErrorHandlers should be used only as template for local ErrorHandlers");
  }

  public Consumer<Exception> routerForChain(MessageProcessorChain chain, Supplier<Consumer<Exception>> errorRouterSupplier) {
    if (!routers.containsKey(chain)) {
      routers.put(chain, newGlobalRouter(errorRouterSupplier.get()));
    }
    return routers.get(chain);
  }

  private Consumer<Exception> newGlobalRouter(Consumer<Exception> router) {
    return new ExceptionRouter() {

      final AtomicBoolean disposed = new AtomicBoolean(false);

      @Override
      public void accept(Exception error) {
        LOGGER.debug("Routing error in '{}'...", this);

        router.accept(error);
      }

      @Override
      public void dispose() {
        if (!disposed.getAndSet(true)) {
          disposeIfNeeded(router, LOGGER);
        }
      }
    };
  }

  @Override
  public void initialise() throws InitialisationException {
    if (!initialised.getAndSet(true)) {
      super.initialise();
    }
  }

  @Override
  public void start() throws MuleException {
    if (started.getAndIncrement() == 0) {
      super.start();
    }
  }

  @Override
  public void stop() throws MuleException {
    if (started.decrementAndGet() == 0) {
      super.stop();
    }
  }

  @Override
  public void dispose() {
    if (started.get() == 0 && initialised.getAndSet(false)) {
      super.dispose();
    }
  }

  public void setFromGlobalErrorHandler() {
    this.getExceptionListeners().stream()
        .filter(TemplateOnErrorHandler.class::isInstance)
        .forEach(exceptionListener -> ((TemplateOnErrorHandler) exceptionListener).setFromGlobalErrorHandler(true));
  }

  public Map<Component, Consumer<Exception>> getRouters() {
    return routers;
  }

  public void clearRouterForChain(MessageProcessorChain chain) {
    routers.remove(chain);
  }

  /**
   * Keeps track of all the components referencing this global error handler.
   *
   * @param location {@link ComponentLocation} of the component referencing this global error handler.
   */
  public void addComponentReference(ComponentLocation location) {
    this.getExceptionListeners().stream()
        .filter(TemplateOnErrorHandler.class::isInstance)
        .forEach(exceptionListener -> ((TemplateOnErrorHandler) exceptionListener)
            .addGlobalErrorHandlerComponentReference(location));
  }

  /**
   * Keeps track of all the top level components referencing this global error handler.
   *
   * @param name name of the top level component referencing this global error handler.
   */
  public void addComponentReference(String name) {
    this.getExceptionListeners().stream()
        .filter(TemplateOnErrorHandler.class::isInstance)
        .forEach(exceptionListener -> ((TemplateOnErrorHandler) exceptionListener)
            .addGlobalErrorHandlerComponentReference(name));
  }

}
