/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension.resolver;


import static org.mule.sdk.api.values.ValueBuilder.getValuesFor;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.values.Value;
import org.mule.sdk.api.values.ValueProvider;

import java.util.Set;

public class WithRequiredParameterSdkValueProvider implements ValueProvider {

  @Parameter
  String requiredValue;

  @Override
  public Set<Value> resolve() {
    return getValuesFor(requiredValue);
  }

  @Override
  public String getId() {
    return getClass().getName();
  }
}
