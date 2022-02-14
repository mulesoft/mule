/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.extension.api.runtime.route.Chain;
import org.mule.runtime.module.extension.internal.runtime.operation.ImmutableProcessorChildContextChainExecutor;

/**
 * An {@link ValueResolver} which wraps the given {@link Processor} in a {@link Chain}, using the event of the current
 * {@link ValueResolvingContext}. This resolver returns new instances per every invocation
 *
 * @since 4.0
 */
public final class ProcessorChainValueResolver implements ValueResolver<Chain> {

  private final StreamingManager streamingManager;

  private final MessageProcessorChain chain;

  /**
   * Creates a resolver for the provided chain executor. The lifecycle of the provided {@code chain} must be managed by the owner
   * of the chain.
   *
   * @param streamingManager
   * @param chain            the chain to create an executor for
   */
  public ProcessorChainValueResolver(StreamingManager streamingManager, final MessageProcessorChain chain) {
    this.streamingManager = streamingManager;
    this.chain = chain;
  }

  /**
   * Returns a {@link Chain} that wraps the given {@link Processor} using the current {@code event}
   *
   * @param context a {@link ValueResolvingContext}
   * @return a {@link Chain}
   * @throws MuleException
   */
  @Override
  public Chain resolve(ValueResolvingContext context) throws MuleException {
    return new ImmutableProcessorChildContextChainExecutor(streamingManager, context.getEvent(), chain);
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isDynamic() {
    return false;
  }

}
