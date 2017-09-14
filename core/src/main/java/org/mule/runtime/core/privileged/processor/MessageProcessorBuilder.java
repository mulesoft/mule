/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor;

import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

/**
 * Builds {@link Processor} instances. Processor builders are useful when a new processor instance is needed for each
 * {@link MessageProcessorChain} it is used in.
 * 
 * @since 3.0
 */
public interface MessageProcessorBuilder {

  /**
   * Build a new {@link Processor} instance.
   *
   * @return new {@link Processor} instance.
   */
  Processor build();
}
