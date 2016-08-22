/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.param;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import org.mule.extension.db.internal.operation.QuerySettings;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Text;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Base class containing common attributes for a statement.
 *
 * @param <T> the generic type of the implementing type
 *           @since 4.0
 */
public abstract class StatementDefinition<T extends StatementDefinition<T>> {

  /**
   * The text of the SQL query to be executed
   */
  @Parameter
  @Optional
  @Text
  @DisplayName("SQL Query Text")
  protected String sql;

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
   * Parameters to configure the query
   */
  @ParameterGroup
  protected QuerySettings settings = new QuerySettings();

  /**
   * Returns a globally defined definition this instance
   * points to. Can be {@code null}.
   * @return Another definition of the same type or {@code null}
   */
  public abstract T getTemplate();

  /**
   * Returns an instance of the same class which state
   * has been derived from the state of the {@link #getTemplate()}
   * and the state of {@code this} instance.
   *
   * If {@link #getTemplate()} is {@code null} then {@code this}
   * instance is returned.
   *
   * This method is recursive in the sense that the template can
   * point to another template itself.
   */
  public T resolveFromTemplate() {
    T template = getTemplate();

    if (template == null) {
      return (T) this;
    } else {
      template = template.resolveFromTemplate();
    }

    T resolvedDefinition = copy();

    if (isBlank(resolvedDefinition.getSql())) {
      resolvedDefinition.setSql(template.getSql());
    }

    resolveTemplateParameters(template, resolvedDefinition);

    return resolvedDefinition;
  }

  /**
   * Returns a {@link Map} which keys are the names of the input
   * parameters and the values are its values
   */
  public Map<String, Object> getParameterValues() {
    return inputParameters.stream().collect(toMap(InputParameter::getParamName, InputParameter::getValue));
  }

  /**
   * Returns a shallow copy of {@code this} instance.
   * @return
   */
  protected T copy() {
    StatementDefinition copy;
    try {
      copy = getClass().newInstance();
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create instance of " + getClass().getName()), e);
    }

    copy.sql = sql;
    copy.inputParameters = new LinkedList<>(inputParameters);

    return (T) copy;
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

  public String getSql() {
    return sql;
  }

  public void setSql(String sql) {
    this.sql = sql;
  }

  public QuerySettings getSettings() {
    return settings;
  }

  public void setSettings(QuerySettings settings) {
    this.settings = settings;
  }
}
