/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.profiling.InternalSpan;

/**
 * Provides the Span from the core event and the component.
 *
 * @since 4.5.0
 */
public interface CoreEventExecutionSpanProvider {

  /**
   * Provides a span for related to a component that is hit by an event.
   *
   * @param coreEvent         the core event that hits the component.
   * @param component         the component hit
   * @param muleConfiguration the mule configuration related to the deployed artifact.
   *
   * @return the {@link InternalSpan} for that coreEvent and component.
   */
  InternalSpan getSpan(CoreEvent coreEvent, Component component, MuleConfiguration muleConfiguration);

  /**
   * Provides a span for related to a component that is hit by an event.
   *
   * @param coreEvent               the core event that hits the component.
   * @param component               the component hit
   * @param muleConfiguration       the mule configuration related to the deployed artifact.
   * @param coreEventSpanCustomizer the {@link CoreEventSpanCustomizer} customizer for the span.
   * @return the {@link InternalSpan} for that coreEvent and component.
   */
  InternalSpan getSpan(CoreEvent coreEvent, Component component, MuleConfiguration muleConfiguration,
                       CoreEventSpanCustomizer coreEventSpanCustomizer);
}
