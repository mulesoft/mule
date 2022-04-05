/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.consumer.tracing.operations;

import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.internal.profiling.consumer.tracing.span.builder.ComponentSpanBuilder;
import org.mule.runtime.core.internal.profiling.consumer.tracing.span.builder.FlowSpanBuilder;
import org.mule.runtime.core.internal.profiling.consumer.tracing.span.builder.SpanBuilder;

/**
 * Utility methods for handling profiling spans
 *
 * @since 4.5.0
 */
public final class SpanUtils {

  private SpanUtils() {}

  public static SpanBuilder getBuilder(ComponentLocation location) {
    if (location.getComponentIdentifier()
        .getType().equals(TypedComponentIdentifier.ComponentType.FLOW)) {
      return FlowSpanBuilder.builder();
    } else {
      return ComponentSpanBuilder.builder();
    }
  }

}
