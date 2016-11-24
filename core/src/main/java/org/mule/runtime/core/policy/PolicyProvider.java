/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import org.mule.runtime.dsl.api.component.ComponentIdentifier;

import java.util.List;

/**
 * Implementation of this interface must provide access to the policies to be applied to message sources or operations.
 *
 * @since 4.0
 */
public interface PolicyProvider {

  /**
   * Creates a collection of {@link Policy} with the policy chain to be applied to a source.
   * <p>
   * The provided collection must be in the correct order in which the policies must be applied.
   *
   * @param sourceIdentifier the identifier of the message source
   * @return a {@link DefaultOperationPolicy} associated to that source.
   */
  List<Policy> findSourceParameterizedPolicies(ComponentIdentifier sourceIdentifier);

  /**
   * Creates a collection of {@link Policy} with the policy chain be applied to an operation.
   * <p>
   * The provided collection must be in the correct order in which the policies must be applied.
   *
   * @param operationIdentifier the identifier of the operation.
   * @return a {@link DefaultOperationPolicy} associated to that source.
   */
  List<Policy> findOperationParameterizedPolicies(ComponentIdentifier operationIdentifier);

}
