/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.mvel.function;

import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static org.mule.runtime.api.metadata.DataType.STRING;
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

  private ConfigurationProperties configurationProperties;

  public PropertyAccessFunction(ConfigurationProperties configurationProperties) {
    this.configurationProperties = configurationProperties;
  }

  @Override
  public Object call(Object[] parameters, BindingContext context) {
    String name = (String) parameters[0];
    String value = configurationProperties.resolveStringProperty(name).orElse(System.getProperty(name));
    if (value == null) {
      value = System.getenv(name);
    }
    return value;
  }

  @Override
  public Optional<DataType> returnType() {
    return of(STRING);
  }

  @Override
  public List<FunctionParameter> parameters() {
    return singletonList(new FunctionParameter("name", STRING));
  }

}
