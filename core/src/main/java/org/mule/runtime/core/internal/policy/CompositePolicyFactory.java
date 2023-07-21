/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.policy;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.policy.OperationPolicyParametersTransformer;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.policy.SourcePolicyParametersTransformer;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.exception.MessagingException;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

class CompositePolicyFactory {

  public SourcePolicy createSourcePolicy(List<Policy> innerKey, ReactiveProcessor flowExecutionProcessor,
                                         Optional<SourcePolicyParametersTransformer> lookupSourceParametersTransformer,
                                         SourcePolicyProcessorFactory sourcePolicyProcessorFactory,
                                         Function<MessagingException, MessagingException> resolver) {
    return new CompositeSourcePolicy(innerKey, flowExecutionProcessor,
                                     lookupSourceParametersTransformer,
                                     sourcePolicyProcessorFactory, resolver);
  }

  public OperationPolicy createOperationPolicy(Component operation, List<Policy> innerKey,
                                               Optional<OperationPolicyParametersTransformer> paramsTransformer,
                                               OperationPolicyProcessorFactory operationPolicyProcessorFactory,
                                               long shutdownTimeout, Scheduler scheduler,
                                               FeatureFlaggingService featureFlaggingService) {
    return new CompositeOperationPolicy(operation, innerKey, paramsTransformer,
                                        operationPolicyProcessorFactory,
                                        shutdownTimeout, scheduler, featureFlaggingService);
  }

}
