/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.params;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.parameterization.ComponentParameterization;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.extension.api.client.params.Parameterizer;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseParameterizer<T extends Parameterizer> implements Parameterizer<T> {

  private final Map<String, Object> rawParameters = new HashMap<>();
  private final Map<Pair<String, String>, Object> groupedParameters = new HashMap<>();

  @Override
  public T withParameter(String parameterName, Object value) {
    rawParameters.put(parameterName, value);
    return (T) this;
  }

  @Override
  public T withParameter(String parameterGroup, String parameter, Object value) {
    groupedParameters.put(new Pair<>(parameterGroup, parameter), value);
    return (T) this;
  }

  public <T extends ComponentModel> void setValuesOn(ComponentParameterization.Builder<T> builder) {
    rawParameters.forEach(builder::withParameter);
    groupedParameters.forEach((pair, value) -> builder.withParameter(pair.getFirst(), pair.getSecond(), value));
  }
}
