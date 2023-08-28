/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.processor.chain.HasMessageProcessors;
import org.mule.runtime.extension.api.runtime.route.Chain;

/**
 * A {@link Chain} that wraps a {@link Processor} and allows to execute it.
 *
 * @since 4.4.0
 */
public interface ProcessorChainExecutor extends Chain, HasMessageProcessors {

  /**
   * @return the event prior to its execution
   */
  CoreEvent getOriginalEvent();
}
