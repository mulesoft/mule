/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.profiling;

import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingEventContext;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A wrapper for making a {@link ProfilingDataProducer} resettable.
 *
 * @since 4.5.0
 */
public class ResettableProfilingDataProducerDelegate<T extends ProfilingEventContext, S>
    implements ResettableProfilingDataProducer<T, S> {

  private final ProfilingDataProducer<T, S> profilingDataProducer;
  private final Consumer<ProfilingDataProducer<T, S>> resetAction;

  public ResettableProfilingDataProducerDelegate(
                                                 ProfilingDataProducer<T, S> profilingDataProducer,
                                                 Consumer<ProfilingDataProducer<T, S>> resetAction) {
    this.profilingDataProducer = profilingDataProducer;
    this.resetAction = resetAction;
  }

  @Override
  public void triggerProfilingEvent(T profilerEventContext) {
    profilingDataProducer.triggerProfilingEvent(profilerEventContext);
  }

  @Override
  public void triggerProfilingEvent(S sourceData, Function<S, T> transformation) {
    profilingDataProducer.triggerProfilingEvent(sourceData, transformation);
  }

  @Override
  public void reset() {
    resetAction.accept(this);
  }

  public ProfilingDataProducer<T, S> getDelegate() {
    return profilingDataProducer;
  }
}
