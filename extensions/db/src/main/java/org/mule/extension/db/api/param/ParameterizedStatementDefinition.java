/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.param;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toMap;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Base class for {@link StatementDefinition} implementations which have
 * a {@link List} of {@link InputParameter}
 * @param <T> the generic type of the implementing type
 * @since 4.0
 */
public abstract class ParameterizedStatementDefinition<T extends ParameterizedStatementDefinition>
    extends StatementDefinition<T> {


  /**
   * A list of input parameters to be set on the JDBC prepared
   * statement. Each parameter should be referenced in the sql
   * text using a semicolon prefix (E.g: {@code where id = :myParamName)})
   */
  @Parameter
  @Optional
  @DisplayName("Input Parameters")
  protected List<InputParameter> inputParameters = new LinkedList<>();


  /**
   * Returns a {@link Map} which keys are the names of the input
   * parameters and the values are its values
   */
  public Map<String, Object> getParameterValues() {
    return inputParameters.stream().collect(toMap(InputParameter::getParamName, InputParameter::getValue));
  }

  /**
   * Optionally returns a parameter of the given {@code name}
   * @param name the name of the searched parameter
   * @return an {@link Optional} {@link QueryParameter}
   */
  public java.util.Optional<QueryParameter> getInputParameter(String name) {
    return findParameter(inputParameters, name);
  }

  /**
   * @return an immutable list with the input parameters
   */
  public List<QueryParameter> getInputParameters() {
    return unmodifiableList(inputParameters);
  }

  private void resolveTemplateParameters(T template, T resolvedDefinition) {
    Map<String, Object> templateParamValues = null;
    if (template != null) {
      template = (T) template.resolveFromTemplate();
      templateParamValues = template.getParameterValues();
    }

    Map<String, Object> resolvedParameterValues = new HashMap<>();
    if (templateParamValues != null) {
      resolvedParameterValues.putAll(templateParamValues);
    }

    resolvedParameterValues.putAll(getParameterValues());
    resolvedDefinition.getInputParameters().forEach(p -> {
      InputParameter inputParameter = (InputParameter) p;
      final String paramName = inputParameter.getParamName();
      if (resolvedParameterValues.containsKey(paramName)) {
        inputParameter.setValue(resolvedParameterValues.get(paramName));
        resolvedParameterValues.remove(paramName);
      }
    });

    resolvedParameterValues.entrySet().stream()
        .map(entry -> {
          InputParameter inputParameter = new InputParameter();
          inputParameter.setParamName(entry.getKey());
          inputParameter.setValue(entry.getValue());

          return inputParameter;
        }).forEach(p -> resolvedDefinition.inputParameters.add(p));
  }

  protected java.util.Optional<QueryParameter> findParameter(List<? extends QueryParameter> parameters, String name) {
    return parameters.stream()
        .filter(p -> p.getParamName().equals(name))
        .map(p -> (QueryParameter) p)
        .findFirst();
  }

  @Override
  protected T copy() {
    T copy = super.copy();
    copy.inputParameters = new LinkedList<>(inputParameters);

    return copy;
  }

  @Override
  public T resolveFromTemplate() {
    T resolved = super.resolveFromTemplate();
    resolveTemplateParameters((T) resolved.getTemplate(), resolved);

    return resolved;
  }
}
