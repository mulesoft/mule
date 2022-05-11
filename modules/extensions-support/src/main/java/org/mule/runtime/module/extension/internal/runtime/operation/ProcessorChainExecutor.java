/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
