/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.client.params;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.parameterization.ComponentParameterization;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.extension.api.client.params.Parameterizer;

import java.util.HashMap;
import java.util.Map;

/**
 * Base implementation of {@link Parameterizer}
 *
 * @since 4.5.0
 */
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
