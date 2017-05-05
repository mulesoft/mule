/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.param;

import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import org.mule.extension.db.internal.domain.metadata.DbInputMetadataResolver;
import org.mule.runtime.extension.api.annotation.metadata.TypeResolver;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base class for {@link StatementDefinition} implementations which have a {@link Map} of input parameters.
 *
 * @param <T> the generic type of the implementing type
 * @since 4.0
 */
public abstract class ParameterizedStatementDefinition<T extends ParameterizedStatementDefinition>
    extends StatementDefinition<T> {


  /**
   * A {@link Map} which keys are the name of an input parameter to be set on the JDBC prepared statement.
   * Each parameter should
   * be referenced in the sql text using a semicolon prefix (E.g: {@code where id = :myParamName)}).
   * <p>
   * The map's values will contain the actual assignation for each parameter.
   */
  @Parameter
  @Content
  @Optional(defaultValue = "#[{}]")
  @TypeResolver(DbInputMetadataResolver.class)
  @Placement(order = 2)
  @Example("#[{'name': \"Max\", 'nickname': \"The Mule\", 'company': \"MuleSoft\"}]")
  protected Map<String, Object> inputParameters = new LinkedHashMap<>();

  /**
   * Returns a {@link Map} which keys are the names of the input parameters and the values are its values
   */
  public Map<String, Object> getParameterValues() {
    return unmodifiableMap(getInputParameters());
  }

  /**
   * Optionally returns a parameter of the given {@code name}
   *
   * @param name the name of the searched parameter
   * @return an {@link Optional} {@link ParameterType}
   */
  public java.util.Optional<Object> getInputParameter(String name) {
    return findParameter(getInputParameters(), name);
  }

  protected java.util.Optional<Object> findParameter(Map<String, Object> parameters, String name) {
    return parameters.containsKey(name) ? of(parameters.get(name)) : empty();
  }

  /**
   * @return an immutable {@link Map} with the input parameters
   */
  public Map<String, Object> getInputParameters() {
    return unmodifiableMap(inputParameters);
  }

  /**
   * Adds a new input parameter
   *
   * @param paramName the parameter name
   * @param value the parameter value
   */
  public void addInputParameter(String paramName, Object value) {
    inputParameters.put(paramName, value);
  }

  @Override
  protected T copy() {
    T copy = super.copy();
    getInputParameters().forEach(copy::addInputParameter);

    return copy;
  }

}
