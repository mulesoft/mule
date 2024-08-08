/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.isSdkApiDefined;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.nested.NestableElementModel;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.extension.api.runtime.route.Chain;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.runtime.operation.ImmutableProcessorChildContextChainExecutor;
import org.mule.runtime.module.extension.internal.runtime.operation.ProcessorChainExecutor;
import org.mule.runtime.module.extension.internal.runtime.operation.SdkProcessorChainExecutorAdapter;

/**
 * An {@link ValueResolver} which wraps the given {@link Processor} into either an extension-api {@link Chain} or a sdk-api
 * {@link org.mule.sdk.api.runtime.route.Chain}, using the event of the current {@link ValueResolvingContext}.
 * <p>
 * Because a different implementation will be returned depending on the API used to write the component, the value resolver uses
 * {@link Object} as its return generic.
 * <p>
 * This resolver returns new instances per every invocation
 *
 * @since 4.0
 */
public final class ProcessorChainValueResolver implements ValueResolver<Object> {

  private final NestableElementModel model;
  private final MessageProcessorChain chain;
  private final StreamingManager streamingManager;

  /**
   * Creates a resolver for the provided chain executor.
   *
   * The lifecycle of the provided {@code chain} must be managed by the owner of the chain.
   *
   * @param model
   * @param streamingManager
   * @param chain            the chain to create an executor for
   */
  public ProcessorChainValueResolver(NestableElementModel model, MessageProcessorChain chain, StreamingManager streamingManager) {
    this.model = model;
    this.chain = chain;
    this.streamingManager = streamingManager;
  }

  /**
   * Returns a {@link Chain} that wraps the given {@link Processor} using the current {@code event}
   *
   * @param context a {@link ValueResolvingContext}
   * @return a {@link Chain}
   * @throws MuleException
   */
  @Override
  public Object resolve(ValueResolvingContext context) throws MuleException {
    ProcessorChainExecutor executor =
        new ImmutableProcessorChildContextChainExecutor(streamingManager, context.getEvent(), chain);
    return isSdkApiDefined(model) ? new SdkProcessorChainExecutorAdapter(executor) : executor;
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isDynamic() {
    return false;
  }

}
