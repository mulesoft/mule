/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.component;

import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.management.stats.ComponentStatistics;
import org.mule.runtime.core.api.processor.Processor;

/**
 * A <code>Component</code> component processes a {@link CoreEvent} by invoking the component instance that has been configured,
 * optionally returning a result.
 * <p/>
 * Implementations of <code>Component</code> can use different types of component implementation, implement component instance
 * pooling or implement <em>bindings</em> which allow for service composition.
 * 
 * @deprecated Transport infrastructure is deprecated.
 */
@Deprecated
public interface Component extends Processor {

  /**
   * Component statistics are used to gather component statistics such as sync/async invocation counts and total and average
   * execution time.
   */
  ComponentStatistics getStatistics();

  @Override
  default ProcessingType getProcessingType() {
    return BLOCKING;
  }

}
