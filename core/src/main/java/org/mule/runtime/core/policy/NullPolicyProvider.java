/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import static java.util.Optional.empty;
import org.mule.runtime.dsl.api.component.ComponentIdentifier;

import java.util.Optional;

/**
 * A policy provider that returns no policy.
 *
 * @since 4.0
 */
public class NullPolicyProvider implements PolicyProvider {

  @Override
  public Optional<Policy> findSourcePolicyInstance(String executionIdentifier, ComponentIdentifier sourceIdentifier) {
    return empty();
  }

  @Override
  public Optional<Policy> findOperationPolicy(String executionIdentifier, ComponentIdentifier operationIdentifier) {
    return empty();
  }

  @Override
  public void dispose(String executionIdentifier) {
    //Do nothing
  }
}
