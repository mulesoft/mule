/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.el.mvel.datatype;

import org.mule.mvel2.compiler.CompiledExpression;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

/**
 * Propagates {@link org.mule.runtime.api.metadata.DataType} from the right to the left side of an assignment expression if
 * possible
 */
public interface EnricherDataTypePropagator {

  /**
   * Propagates {@link org.mule.runtime.api.metadata.DataType} on message enrichment
   *
   * @param event event being enriched
   * @param typedValue value used to enrich the message
   * @param compiledExpression assignment expression used for enrichment
   * @return true if propagation was done, false otherwise
   */
  boolean propagate(PrivilegedEvent event, PrivilegedEvent.Builder builder, TypedValue typedValue,
                    CompiledExpression compiledExpression);
}
