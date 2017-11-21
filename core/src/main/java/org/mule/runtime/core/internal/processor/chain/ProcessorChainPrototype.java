/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.chain;

import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChainBuilder;

/**
 * Allows to create a new {@link MessageProcessorChainBuilder} with the same {@link Processor}s as this chain.
 *
 * @since 4.1
 */
public interface ProcessorChainPrototype extends MessageProcessorChain {

  /**
   * @return a new builder with the same {@link Processor}s as this chain.
   */
  MessageProcessorChainBuilder toChainBuilder();
}