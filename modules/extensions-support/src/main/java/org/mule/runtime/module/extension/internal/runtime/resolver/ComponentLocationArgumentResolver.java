/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
