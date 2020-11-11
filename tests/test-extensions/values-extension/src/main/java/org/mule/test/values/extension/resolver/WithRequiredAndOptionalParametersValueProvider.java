/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension.resolver;

import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.values.ValueProvider;

import java.util.Set;

import org.apache.logging.log4j.util.Strings;

import static org.mule.runtime.extension.api.values.ValueBuilder.getValuesFor;

public class WithRequiredAndOptionalParametersValueProvider implements ValueProvider {

  @Parameter
  String requiredValue;

  @Parameter
  @Optional
  String optionalValue;

  @Override
  public Set<Value> resolve() {
    if (Strings.isBlank(optionalValue)) {
      return getValuesFor("requiredValue:" + requiredValue);
    }
    return getValuesFor("requiredValue:" + requiredValue, "optionalValue:" + optionalValue);
  }
}
