/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import org.mule.runtime.api.dsl.config.ComponentIdentifier;
import org.mule.runtime.api.message.Attributes;

/**
 * Factory for creating {@link PolicyPointcutParameters} for an specific source.
 *
 * @since 4.0
 */
public interface SourcePolicyPointcutParametersFactory {

  /**
   * @return true if this factory can create {@link PolicyPointcutParameters} for the source identifier, false otherwise.
   */
  boolean supportsSourceIdentifier(ComponentIdentifier sourceIdentifier);

  /**
   * Creates an specific {@link PolicyPointcutParameters} for a particular source operation by {@code sourceIdentifier}.
   *
   * @param flowName name of the flow where the source is defined. Not empty.
   * @param sourceIdentifier identifier of the message source
   * @param attributes the attributes from the message generated by the message source
   * @return a {@link PolicyPointcutParameters} with custom parameters associated to the {@code sourceIdentifier}
   */
  PolicyPointcutParameters createPolicyPointcutParameters(String flowName, ComponentIdentifier sourceIdentifier,
                                                          Attributes attributes);

}
