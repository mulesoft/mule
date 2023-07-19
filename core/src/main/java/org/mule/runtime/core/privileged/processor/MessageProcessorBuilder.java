/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
