/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.resolver;

import static org.mule.sdk.api.values.ValueBuilder.getValuesFor;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.values.Value;
import org.mule.sdk.api.values.ValueProvider;
import org.mule.sdk.api.values.ValueResolvingException;

import java.util.Set;

public class WithFourActingParametersValueProvider implements ValueProvider {

  @Parameter
  private String requiredValue;

  @Parameter
  private String anotherValue;

  @Parameter
  private String someValue;

  @Parameter
  @Optional
  private String optionalValue;

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    return getValuesFor(requiredValue, anotherValue, someValue, optionalValue);
  }

  @Override
  public String getId() {
    return getClass().getName();
  }

}
