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

  /**
   * Creates a policy chain to be applied to a source.
   *
   * @param sourceIdentifier the identifier of the message source
   * @return a {@link OperationPolicy} associated to that source.
   */
  Optional<PolicyChain> findSourcePolicyChain(ComponentIdentifier sourceIdentifier);

  /**
   * Creates a policy chain to be applied to an operation.
   *
   * @param operationIdentifier the identifier of the operation.
   * @return a {@link OperationPolicy} associated to that source.
   */
  Optional<PolicyChain> findOperationPolicyChain(ComponentIdentifier operationIdentifier);

}
