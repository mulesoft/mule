/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.resolver;

import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.values.ValueProvider;

import java.util.Set;

import static java.util.Collections.emptySet;
import org.apache.logging.log4j.util.Strings;
import static org.mule.runtime.extension.api.values.ValueBuilder.getValuesFor;

public class WithOptionalParametersWithDefaultValueProvider implements ValueProvider {

  @Parameter
  @Optional(defaultValue = "VP_DEFAULT_VALUE")
  String optionalValue;

  @Override
  public Set<Value> resolve() {
    if (Strings.isBlank(optionalValue)) {
      return emptySet();
    }
    return getValuesFor("optionalValue:" + optionalValue);
  }
}
