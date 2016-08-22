/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.param;

import static java.util.Collections.unmodifiableList;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.util.LinkedList;
import java.util.List;

/**
 * The definition of the invocation to a stored procedure
 *
 * @since 4.0
 */
public class StoredProcedureCall extends StatementDefinition<StoredProcedureCall> {

  /**
   * A list of output parameters to be set on the JDBC prepared
   * statement. Each parameter should be referenced in the sql
   * text using a semicolon prefix (E.g: {@code call multiply(:value, :result)})
   */
  @Parameter
  @Optional
  private List<OutputParameter> outputParameters = new LinkedList<>();

  /**
   * A list of parameters to be set on the JDBC prepared
   * statement which are both input and output. Each parameter
   * should be referenced in the sql text using a semicolon
   * prefix (E.g: {@code call increment(:value))
   */
  @Parameter
  @Optional
  private List<InputParameter> inOutParameters = new LinkedList<>();

  @Parameter
  @Optional
  @XmlHints(allowInlineDefinition = false)
  private StoredProcedureCall template;

  public java.util.Optional<QueryParameter> getOutputParameter(String name) {
    return findParameter(outputParameters, name);
  }

  public java.util.Optional<QueryParameter> getInOutParameter(String name) {
    return findParameter(inOutParameters, name);
  }

  public List<OutputParameter> getOutputParameters() {
    return unmodifiableList(outputParameters);
  }

  public List<InputParameter> getInOutParameters() {
    return unmodifiableList(inOutParameters);
  }

  @Override
  public StoredProcedureCall getTemplate() {
    return template;
  }
}
