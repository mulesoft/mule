/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static com.google.common.collect.ImmutableMap.of;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Runtime.getRuntime;
import static java.lang.Thread.currentThread;
import static org.mule.runtime.core.internal.event.EventQuickCopy.quickCopy;
import static reactor.core.publisher.Mono.create;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.danielw.fop.ObjectFactory;
import cn.danielw.fop.ObjectPool;
import cn.danielw.fop.PoolConfig;
import cn.danielw.fop.Poolable;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.MonoSink;

/**
 * Common behavior for flow dispatching, whether policies are applied or not.
 */
class CommonSourcePolicy {

  private static final Logger LOGGER = LoggerFactory.getLogger(CommonSourcePolicy.class);

  public static final String POLICY_SOURCE_PARAMETERS_PROCESSOR = "policy.source.parametersProcessor";
  public static final String POLICY_SOURCE_CALLER_SINK = "policy.source.callerSink";

  private final ObjectPool<FluxSink<CoreEvent>> policySinkPool;

  CommonSourcePolicy(ObjectFactory<FluxSink<CoreEvent>> sinkFactory) {
    PoolConfig config = new PoolConfig()
        .setPartitionSize(getRuntime().availableProcessors())
        .setMaxSize(1)
        .setMinSize(1)
        .setMaxIdleMilliseconds(MAX_VALUE)
        .setScavengeIntervalMilliseconds(0);

    policySinkPool = new ObjectPool<>(config, sinkFactory);

  }

  public Publisher<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>> process(CoreEvent sourceEvent,
                                                                                         MessageSourceResponseParametersProcessor respParamProcessor) {
    return create(callerSink -> {
      try (Poolable<FluxSink<CoreEvent>> policySink = policySinkPool.borrowObject()) {
        policySink.getObject().next(quickCopy(sourceEvent, of(POLICY_SOURCE_PARAMETERS_PROCESSOR, respParamProcessor,
                                                              POLICY_SOURCE_CALLER_SINK, callerSink)));
      }
    });
  }

  public MessageSourceResponseParametersProcessor getResponseParamsProcessor(CoreEvent event) {
    return ((InternalEvent) event).getInternalParameter(POLICY_SOURCE_PARAMETERS_PROCESSOR);
  }

  public void finishFlowProcessing(CoreEvent event, Either<SourcePolicyFailureResult, SourcePolicySuccessResult> result) {
    if (!((BaseEventContext) event.getContext()).isComplete()) {
      ((BaseEventContext) event.getContext()).success(event);
    }

    ((MonoSink<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>>) ((InternalEvent) event)
        .getInternalParameter(POLICY_SOURCE_CALLER_SINK)).success(result);
  }

  public void finishFlowProcessing(CoreEvent event, Either<SourcePolicyFailureResult, SourcePolicySuccessResult> result,
                                   Throwable error) {
    if (!((BaseEventContext) event.getContext()).isComplete()) {
      ((BaseEventContext) event.getContext()).error(error);
    }

    ((MonoSink<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>>) ((InternalEvent) event)
        .getInternalParameter(POLICY_SOURCE_CALLER_SINK)).success(result);
  }

  public void dispose() {
    try {
      policySinkPool.shutdown();
    } catch (InterruptedException e) {
      LOGGER.debug("Pool shutdown interrupted.");
      currentThread().interrupt();
    }
  }
}
