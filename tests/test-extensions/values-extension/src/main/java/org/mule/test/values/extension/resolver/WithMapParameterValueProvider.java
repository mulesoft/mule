/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension.resolver;

import static org.mule.sdk.api.values.ValueBuilder.getValuesFor;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.values.Value;
import org.mule.sdk.api.values.ValueProvider;
import org.mule.sdk.api.values.ValueResolvingException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class WithMapParameterValueProvider implements ValueProvider {


  @Parameter
  private Map<String, String> requiredValue;

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    List<String> mapEntries = requiredValue.entrySet().stream().map(entry -> entry.getKey() + " : " + entry.getValue())
        .collect(Collectors.toList());
    return getValuesFor(mapEntries);
  }

  @Override
  public String getId() {
    return getClass().getName();
  }

}
