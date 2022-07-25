/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.event.CoreEvent;

/**
 * A Customizer for the creation of {@link InternalSpan}.
 *
 * @since 4.5.0
 */
public interface CoreEventSpanCustomizer {

  /**
   * Gets the {@link InternalSpan} name from the {@param coreEvent} andthe {@param component}
   *
   * @param coreEvent the {@link CoreEvent} to resolve the span name from.
   * @param component the {@link Component} to resolve the span name from.
   * @return
   */
  String getName(CoreEvent coreEvent, Component component);

}
