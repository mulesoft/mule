/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
