/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.param;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * The definition of the invocation to a stored procedure
 *
 * @since 4.0
 */
public class StoredProcedureCall extends ParameterizedStatementDefinition<StoredProcedureCall> {

  /**
   * A {@link Map} which keys are the name of a parameter to be set on the JDBC prepared statement which is both input and output.
   * <p>
   * Each parameter should be referenced in the sql text using a semicolon prefix (E.g: {@code where id = :myParamName)}).
   * <p>
   * The map's values will contain the actual assignation for each parameter.
   */
  @Parameter
  @Optional
  @DisplayName("Input - Output Parameters")
  @XmlHints(allowReferences = false)
  protected LinkedHashMap<String, Object> inOutParameters = new LinkedHashMap<>();
  /**
   * A list of output parameters to be set on the JDBC prepared
   * statement. Each parameter should be referenced in the sql
   * text using a semicolon prefix (E.g: {@code call multiply(:value, :result)})
   */
  @Parameter
  @Optional
  @DisplayName("Output Parameters")
  @XmlHints(allowReferences = false)
  private List<OutputParameter> outputParameters = new LinkedList<>();

  @Override
  protected StoredProcedureCall copy() {
    StoredProcedureCall copy = super.copy();
    copy.outputParameters = new LinkedList<>(outputParameters);
    copy.inOutParameters = new LinkedHashMap<>(inOutParameters);

    return copy;
  }

  public java.util.Optional<OutputParameter> getOutputParameter(String name) {
    return outputParameters.stream().filter(p -> p.getKey().equals(name)).findFirst();
  }

  public java.util.Optional<Object> getInOutParameter(String name) {
    return findParameter(inOutParameters, name);
  }

  public List<ParameterType> getOutputParameters() {
    return unmodifiableList(outputParameters);
  }

  public Map<String, Object> getInOutParameters() {
    return unmodifiableMap(inOutParameters);
  }

}
