/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.param;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Optional;

/**
 * The definition for a DML query
 *
 * @since 4.0
 */
@Alias("query")
public class QueryDefinition extends StatementDefinition<QueryDefinition> {

  /**
   * A reference to a globally defined query
   * to be used as a template
   */
  @Parameter
  @Optional
  @XmlHints(allowInlineDefinition = false)
  private QueryDefinition template;

  @Override
  protected QueryDefinition copy() {
    QueryDefinition copy = super.copy();
    copy.template = template;

    return copy;
  }

  @Override
  public QueryDefinition getTemplate() {
    return template;
  }

}
