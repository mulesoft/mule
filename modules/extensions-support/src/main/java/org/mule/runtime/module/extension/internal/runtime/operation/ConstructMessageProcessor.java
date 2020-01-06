/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.mule.runtime.api.util.collection.SmallMap.of;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.internal.event.EventQuickCopy.quickCopy;
import static org.mule.runtime.core.internal.policy.PolicyNextActionMessageProcessor.POLICY_IS_PROPAGATE_MESSAGE_TRANSFORMATIONS;
import static org.mule.runtime.core.internal.policy.PolicyNextActionMessageProcessor.POLICY_NEXT_OPERATION;
import static org.mule.runtime.module.extension.internal.runtime.operation.ImmutableProcessorChainExecutor.INNER_CHAIN_CTX_MAPPING;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.subscriberContext;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.core.internal.processor.ParametersResolverProcessor;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.function.Function;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.util.context.Context;

/**
 * An implementation of a {@link ComponentMessageProcessor} for {@link ConstructModel construct models}
 *
 * @since 4.0
 */
public class ConstructMessageProcessor extends ComponentMessageProcessor<ConstructModel>
    implements Processor, ParametersResolverProcessor<ConstructModel> {

  public ConstructMessageProcessor(ExtensionModel extensionModel,
                                   ConstructModel constructModel,
                                   ConfigurationProvider configurationProvider,
                                   String target,
                                   String targetValue,
                                   ResolverSet resolverSet,
                                   CursorProviderFactory cursorProviderFactory,
                                   RetryPolicyTemplate retryPolicyTemplate,
                                   ExtensionManager extensionManager,
                                   PolicyManager policyManager,
                                   ReflectionCache reflectionCache) {
    super(extensionModel, constructModel, configurationProvider, target, targetValue, resolverSet, cursorProviderFactory,
          retryPolicyTemplate, extensionManager, policyManager, reflectionCache);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    Flux<CoreEvent> flux = from(publisher)
        .flatMap(event -> subscriberContext().map(ctx -> addContextToEvent(event, ctx)));

    return super.apply(flux);
  }

  private CoreEvent addContextToEvent(CoreEvent event, Context ctx) {
    if (ctx.hasKey(POLICY_NEXT_OPERATION) || ctx.hasKey(POLICY_IS_PROPAGATE_MESSAGE_TRANSFORMATIONS)) {
      Function<Context, Context> context = innerChainCtx -> {
        if (ctx.hasKey(POLICY_NEXT_OPERATION)) {
          innerChainCtx = innerChainCtx.put(POLICY_NEXT_OPERATION, ctx.get(POLICY_NEXT_OPERATION));
        }
        if (ctx.hasKey(POLICY_IS_PROPAGATE_MESSAGE_TRANSFORMATIONS)) {
          innerChainCtx = innerChainCtx.put(POLICY_IS_PROPAGATE_MESSAGE_TRANSFORMATIONS,
                                            ctx.get(POLICY_IS_PROPAGATE_MESSAGE_TRANSFORMATIONS));
        }
        return innerChainCtx;
      };
      return quickCopy(event, of(INNER_CHAIN_CTX_MAPPING, context));
    } else {
      return event;
    }
  }

  @Override
  protected void validateOperationConfiguration(ConfigurationProvider configurationProvider) {
    // Constructs are config-less
  }

  @Override
  public ProcessingType getInnerProcessingType() {
    return CPU_LITE;
  }

}
