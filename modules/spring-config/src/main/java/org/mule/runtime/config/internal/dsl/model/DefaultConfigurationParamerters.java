/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.config.api.dsl.model.ConfigurationParameters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultConfigurationParamerters implements ConfigurationParameters {

  private Map<String, Object> simpleConfigurationParameters;
  private MultiMap<ComponentIdentifier, ConfigurationParameters> complexConfigurationParameters;

  private DefaultConfigurationParamerters(Map<String, Object> simpleConfigurationParameters,
                                          MultiMap<ComponentIdentifier, ConfigurationParameters> complexConfigurationParameters) {
    this.simpleConfigurationParameters = simpleConfigurationParameters;
    this.complexConfigurationParameters = complexConfigurationParameters;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public String getStringParameter(String parameterName) {
    return simpleConfigurationParameters.get(parameterName).toString();
  }

  @Override
  public List<ConfigurationParameters> getComplexConfigurationParameter(ComponentIdentifier componentIdentifier) {
    return complexConfigurationParameters.getAll(componentIdentifier);
  }

  @Override
  public List<Pair<ComponentIdentifier, ConfigurationParameters>> getComplexConfigurationParameters() {
    return complexConfigurationParameters.entryList().stream().map(entry -> new Pair<>(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
  }

  public static class Builder {

    private Map<String, Object> simpleConfigurationParameters = new HashMap<>();
    private MultiMap<ComponentIdentifier, ConfigurationParameters> complexConfigurationParameters = new MultiMap<>();

    private Builder() {}

    public Builder withSimpleParameter(String parameterName, Object parameterValue) {
      this.simpleConfigurationParameters.put(parameterName, parameterValue);
      return this;
    }

    public Builder withComplexParameter(ComponentIdentifier componentIdentifier,
                                        ConfigurationParameters configurationParameters) {
      this.complexConfigurationParameters.put(componentIdentifier, configurationParameters);
      return this;
    }

    public ConfigurationParameters build() {
      return new DefaultConfigurationParamerters(simpleConfigurationParameters, complexConfigurationParameters);
    }

  }

}
