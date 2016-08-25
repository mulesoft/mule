/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.param;

import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

import java.util.LinkedList;
import java.util.List;

/**
 * The definition of a bulk operations defined around one single SQL command
 *
 * @since 4.0
 */
public class BulkQueryDefinition extends StatementDefinition<BulkQueryDefinition> {

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
   * A reference to a globally defined query
   * to be used as a template
   */
  @Parameter
  @Optional
  @XmlHints(allowInlineDefinition = false)
  private BulkQueryDefinition template;

  @Override
  public BulkQueryDefinition getTemplate() {
    return template;
  }

  public java.util.Optional<ParameterType> getParameterType(String paramName) {
    return parameterTypes.stream().filter(p -> p.getParamName().equals(paramName)).findFirst();
  }

  @Override
  protected BulkQueryDefinition copy() {
    BulkQueryDefinition copy = super.copy();
    copy.parameterTypes = new LinkedList<>(parameterTypes);
    copy.template = template;

    return copy;
  }
}
