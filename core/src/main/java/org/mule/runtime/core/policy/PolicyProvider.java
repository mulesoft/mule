/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import org.mule.runtime.dsl.api.component.ComponentIdentifier;

import java.util.Optional;

/**
 * Implementation of this interface must provide access to the policies to be applied to message sources or operations.
 *
 * @since 4.0
 */
public interface PolicyProvider {

  // TODO update
  /**
   * Creates a policy to be applied to a source. The creation must have into consideration the {@code executionIdentifier} to keep
   * track of the state of the policy. A source policy state may be propagated to an operation policy instance. For the same
   * message processing, the same {@code executionIdentifier} will be used.
   * 
   * @param sourceIdentifier the identifier of the message source
   * @return a {@link OperationPolicy} associated to that source.
   */
  Optional<AbstractPolicyChain> findSourcePolicyInstance(ComponentIdentifier sourceIdentifier);

  // TODO update
  /**
   * Creates a policy to be applied to an operation. The creation must have into consideration the {@code executionIdentifier} to
   * keep track of the state of the policy and correlate any previous policy state for the same execution. For the same message
   * processing, the same {@code executionIdentifier} will be used.
   *
   * @param operationIdentifier the identifier of the operation.
   * @return a {@link OperationPolicy} associated to that source.
   */
  Optional<AbstractPolicyChain> findOperationPolicy(ComponentIdentifier operationIdentifier);

}
