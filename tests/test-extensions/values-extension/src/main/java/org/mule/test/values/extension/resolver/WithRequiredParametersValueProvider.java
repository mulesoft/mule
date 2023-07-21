/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.resolver;

import static org.mule.runtime.extension.api.values.ValueBuilder.getValuesFor;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.sdk.api.annotation.param.Optional;

import java.util.List;
import java.util.Set;

public class WithRequiredParametersValueProvider implements ValueProvider {

  @Optional
  @Parameter
  String requiredString;

  @Parameter
  int requiredInteger;

  @Parameter
  boolean requiredBoolean;

  @Parameter
  List<String> strings;

  @Override
  public Set<Value> resolve() {
    return getValuesFor("requiredString:" + requiredString, "requiredInteger:" + requiredInteger,
                        "requiredBoolean:" + requiredBoolean, "strings:" + strings);
  }
}
