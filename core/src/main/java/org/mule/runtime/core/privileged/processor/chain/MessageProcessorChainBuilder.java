/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor.chain;


import org.mule.runtime.core.privileged.processor.MessageProcessorBuilder;
import org.mule.runtime.core.api.processor.Processor;

/**
 * Builds {@link MessageProcessorChain} instances.
 *
 * @since 3.1
 */
public interface MessageProcessorChainBuilder extends MessageProcessorBuilder {

  /**
   * Chain a {@link Processor} by adding it the the list of processors that the builder implementation will use to construct a
   * {@link MessageProcessorChain}
   * 
   * @param processors {@link Processor} instance(s) to be used in the construction of a {@link MessageProcessorChain}
   * @return the current {@link MessageProcessorBuilder} instance.
   */
  MessageProcessorChainBuilder chain(Processor... processors);

  /**
   * Chain a {@link MessageProcessorBuilder} by adding it the the list of processors builders that the builder implementation will
   * use to construct a {@link MessageProcessorChain}. The {@link MessageProcessorBuilder#build()} method is invoked when the
   * chain is constructed.
   *
   * @param builders {@link MessageProcessorBuilder} instance(s) to be used in the construction of a {@link MessageProcessorChain}
   * @return the current {@link MessageProcessorBuilder} instance.
   */
  MessageProcessorChainBuilder chain(MessageProcessorBuilder... builders);

  /**
   * Build a new {@link MessageProcessorBuilder}
   * 
   * @return a new {@link MessageProcessorBuilder} instance.
   */
  @Override
  MessageProcessorChain build();
}
