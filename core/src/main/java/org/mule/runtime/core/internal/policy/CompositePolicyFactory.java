/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.config.FeatureFlaggingService;
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
