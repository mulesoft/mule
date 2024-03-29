/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;

/**
 * {@link ArgumentResolver} which returns the {@link ComponentLocation} of the received {@link ExecutionContextAdapter}
 *
 * @since 4.0
 */
public class ComponentLocationArgumentResolver implements ArgumentResolver<ComponentLocation> {

  @Override
  public ComponentLocation resolve(ExecutionContext executionContext) {
    return ((ExecutionContextAdapter) executionContext).getComponent().getLocation();
  }
}
