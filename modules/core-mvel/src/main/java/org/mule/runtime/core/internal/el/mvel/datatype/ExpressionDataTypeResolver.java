/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.el.mvel.datatype;

import org.mule.mvel2.compiler.CompiledExpression;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

/**
 * Resolves {@link DataType} from a compiled MEL expression
 */
public interface ExpressionDataTypeResolver {

  /**
   * Resolves the data type form a given expression
   *
   * @param event              mule message being executed
   * @param compiledExpression compiled MEL expression
   * @return a non null data type corresponding to the expression if the resolver is able to resolve it, null otherwise
   */
  DataType resolve(PrivilegedEvent event, CompiledExpression compiledExpression);
}
