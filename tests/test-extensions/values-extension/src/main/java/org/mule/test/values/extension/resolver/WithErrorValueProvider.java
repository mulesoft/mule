/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.resolver;


import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.values.ValueResolvingException;
import org.mule.sdk.api.values.Value;
import org.mule.sdk.api.values.ValueProvider;

import java.util.Set;

public class WithErrorValueProvider implements ValueProvider {

  public static final String ERROR_MESSAGE = "Error!!!";

  @Parameter
  private String errorCode;

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    throw new ValueResolvingException(ERROR_MESSAGE, errorCode);
  }

  @Override
  public String getId() {
    return "WithErrorValueProvider-id";
  }
}
