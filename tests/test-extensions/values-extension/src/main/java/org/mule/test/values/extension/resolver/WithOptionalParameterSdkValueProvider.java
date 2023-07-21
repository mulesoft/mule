/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.resolver;


import static org.mule.sdk.api.values.ValueBuilder.getValuesFor;

import java.util.Set;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.values.Value;
import org.mule.sdk.api.values.ValueProvider;

public class WithOptionalParameterSdkValueProvider implements ValueProvider {

  @Optional
  @Parameter
  String optionalValue;

  @Override
  public Set<Value> resolve() {
    return getValuesFor(optionalValue == null ? "Optional value ommited" : optionalValue);
  }

  @Override
  public String getId() {
    return getClass().getName();
  }
}
