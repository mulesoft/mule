/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.param;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED;
import org.mule.extension.db.internal.operation.QuerySettings;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Text;

import java.util.LinkedList;
import java.util.List;

/**
 * Base class containing common attributes for a statement.
 *
 * @param <T> the generic type of the implementing type
 * @since 4.0
 */
public abstract class StatementDefinition<T extends StatementDefinition> {

  /**
   * The text of the SQL query to be executed
   */
  @Parameter
  @Optional
  @Text
  @DisplayName("SQL Query Text")
  protected String sql;


  /**
   * Parameters to configure the query
   */
  @ParameterGroup
  protected QuerySettings settings = new QuerySettings();

  /**
   * Allows to optionally specify the type of one or more of the parameters
   * in the query. If provided, you're not even required to reference
   * all of the parameters, but you cannot reference a parameter not
   * present in the input values
   */
  @Parameter
  @Optional
  @Placement(group = ADVANCED)
  private List<ParameterType> parameterTypes = new LinkedList<>();

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
      template = (T) template.resolveFromTemplate();
    }

    T resolvedDefinition = copy();

    if (isBlank(resolvedDefinition.getSql())) {
      resolvedDefinition.setSql(template.getSql());
    }

    return resolvedDefinition;
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
    copy.parameterTypes = new LinkedList<>(parameterTypes);
    copy.setSettings(settings);
    return (T) copy;
  }


  /**
   * Returns the type for a given parameter
   * @param paramName the parameter's name
   * @return an optional {@link ParameterType}
   */
  public java.util.Optional<ParameterType> getParameterType(String paramName) {
    return parameterTypes.stream().filter(p -> p.getKey().equals(paramName)).findFirst();
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
