/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import org.mule.runtime.core.api.policy.OperationPolicyParametersTransformer;
import org.mule.runtime.core.api.policy.SourcePolicyParametersTransformer;
import org.mule.runtime.dsl.api.component.ComponentIdentifier;

import java.util.Optional;

/**
 * Manager for handling policies in Mule.
 * 
 * Implementation of this class will be used to lookup for {@code Policy}s that must be applied to {@code MessageSource}es or
 * {@code Processor}s.
 * 
 * @since 4.0
 */
public interface PolicyManager {

  /**
   * Creates a policy to be applied to a source. The creation must have into consideration the {@code executionIdentifier} to keep
   * track of the state of the policy. A source policy state may be propagated to an operation policy instance. For the same
   * message processing, the same {@code executionIdentifier} will be used.
   *
   * @param executionIdentifier the unique identifier associated to the exection of a policy instance.
   * @param sourceIdentifier the identifier of the message source
   * @return a {@link SourcePolicy} associated to that source.
   */
  Optional<SourcePolicy> findSourcePolicyInstance(String executionIdentifier, ComponentIdentifier sourceIdentifier);

  /**
   * Creates a policy to be applied to an operation. The creation must have into consideration the {@code executionIdentifier} to
   * keep track of the state of the policy and correlate any previous policy state for the same execution. For the same message
   * processing, the same {@code executionIdentifier} will be used.
   *
   * @param executionIdentifier the unique identifier associated to the exection of a policy instance.
   * @param operationIdentifier the identifier of the operation.
   * @return a {@link OperationPolicy} associated to that source.
   */
  Optional<OperationPolicy> findOperationPolicy(String executionIdentifier, ComponentIdentifier operationIdentifier);

  /**
   * A transformer to map source response function parameters to content in a {@link org.mule.runtime.api.message.Message} and
   * viceversa.
   *
   * @param componentIdentifier the operation identifier.
   * @return a transformer for the source response function. May be an empty null if there's no transformer for the provided
   *         component identifier
   */
  Optional<SourcePolicyParametersTransformer> lookupSourceParametersTransformer(ComponentIdentifier componentIdentifier);

  /**
   * A transformer to map operation parameters to content in a {@link org.mule.runtime.api.message.Message} and viceversa.
   *
   * @param componentIdentifier the operation identifier.
   * @return a transformer for operations. May be an empty null if there's no transformer for the provided component identifier
   */
  Optional<OperationPolicyParametersTransformer> lookupOperationParametersTransformer(ComponentIdentifier componentIdentifier);

  /**
   * Disposes any resource associated to the given {@code executionIdentifier}. Any exception generated by this method will be
   * just logged and discarded.
   *
   * @param executionIdentifier identifier used for previous policies executions.
   */
  void disposePoliciesResources(String executionIdentifier);

}
