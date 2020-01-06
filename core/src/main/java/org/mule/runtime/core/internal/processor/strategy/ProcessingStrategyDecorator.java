/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.construct.BackPressureReason;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;

import java.util.concurrent.RejectedExecutionException;

import org.reactivestreams.Publisher;

/**
 * Base class for {@link ProcessingStrategy} decorators.
 * <p>
 * This class implements {@link Lifecycle} so that all phases are propagated to the {@link #delegate} if it
 * implements the corresponding interfaces.
 *
 * @since 4.3.0
 */
public abstract class ProcessingStrategyDecorator implements ProcessingStrategy, Lifecycle {

  protected final ProcessingStrategy delegate;

  public ProcessingStrategyDecorator(ProcessingStrategy delegate) {
    this.delegate = delegate;
  }

  @Override
  public Sink createSink(FlowConstruct flowConstruct, ReactiveProcessor pipeline) {
    return delegate.createSink(flowConstruct, pipeline);
  }

  @Override
  public void registerInternalSink(Publisher<CoreEvent> flux, String sinkRepresentation) {
    delegate.registerInternalSink(flux, sinkRepresentation);
  }

  @Override
  public Publisher<CoreEvent> registerInternalFlux(Publisher<CoreEvent> flux) {
    return delegate.registerInternalFlux(flux);
  }

  @Override
  public ReactiveProcessor onPipeline(ReactiveProcessor pipeline) {
    return delegate.onPipeline(pipeline);
  }

  @Override
  public ReactiveProcessor onProcessor(ReactiveProcessor processor) {
    return delegate.onProcessor(processor);
  }

  @Override
  public boolean isSynchronous() {
    return delegate.isSynchronous();
  }

  @Override
  public void checkBackpressureAccepting(CoreEvent event) throws RejectedExecutionException {
    delegate.checkBackpressureAccepting(event);
  }

  @Override
  public BackPressureReason checkBackpressureEmitting(CoreEvent event) {
    return delegate.checkBackpressureEmitting(event);
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(delegate);
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(delegate);
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(delegate);
  }

  @Override
  public void dispose() {
    if (delegate instanceof Disposable) {
      ((Disposable) delegate).dispose();
    }
  }
}
