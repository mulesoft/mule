/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.el.function;

import static org.mule.runtime.api.metadata.DataType.STRING;

import static java.util.Collections.singletonList;
import static java.util.Optional.of;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionFunction;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.FunctionParameter;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of p(), a function which returns the value of a property, if any, first checking the configuration ones, then
 * the system ones and finally the environment ones.
 *
 * @since 4.0
 */
public class PropertyAccessFunction implements ExpressionFunction {

  private volatile ConfigurationProperties configurationProperties;

  @Override
  public Object call(Object[] parameters, BindingContext context) {
    String name = (String) parameters[0];
    return configurationProperties.resolveStringProperty(name).orElse(null);
  }

  @Override
  public Optional<DataType> returnType() {
    return of(STRING);
  }

  @Override
  public List<FunctionParameter> parameters() {
    return singletonList(new FunctionParameter("name", STRING));
  }

  public void setConfigurationProperties(ConfigurationProperties configurationProperties) {
    this.configurationProperties = configurationProperties;
  }
}
