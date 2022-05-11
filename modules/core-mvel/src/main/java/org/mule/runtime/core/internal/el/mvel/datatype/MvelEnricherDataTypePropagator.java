/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.el.mvel.datatype;

import org.mule.mvel2.compiler.CompiledExpression;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Propagates {@link DataType} on enrichment expressions using a list of {@link EnricherDataTypePropagator}
 */
public class MvelEnricherDataTypePropagator {

  private final List<EnricherDataTypePropagator> propagators;

  public MvelEnricherDataTypePropagator() {
    this(getDefaultDataTypePropagators());
  }

  public MvelEnricherDataTypePropagator(List<EnricherDataTypePropagator> propagators) {
    this.propagators = new LinkedList<>(propagators);
  }

  private static List<EnricherDataTypePropagator> getDefaultDataTypePropagators() {
    List<EnricherDataTypePropagator> propagators;
    propagators = new LinkedList<>();
    propagators.add(new PayloadEnricherDataTypePropagator());
    propagators.add(new PropertyEnricherDataTypePropagator());
    propagators.add(new FlowVarEnricherDataTypePropagator());
    propagators.add(new SessionVarEnricherDataTypePropagator());

    return propagators;
  }

  public void propagate(TypedValue typedValue, PrivilegedEvent event, PrivilegedEvent.Builder builder,
                        Serializable serializedExpression) {
    if (serializedExpression instanceof CompiledExpression) {
      CompiledExpression compiledExpression = (CompiledExpression) serializedExpression;

      for (EnricherDataTypePropagator propagator : propagators) {
        if (propagator.propagate(event, builder, typedValue, compiledExpression)) {
          return;
        }
      }
    }
  }
}
