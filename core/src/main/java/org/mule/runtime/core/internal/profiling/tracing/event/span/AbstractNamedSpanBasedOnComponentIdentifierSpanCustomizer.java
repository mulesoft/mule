/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.tracing.event.span;

import org.mule.runtime.api.component.Component;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.profiling.tracing.ChildSpanInfo;
import org.mule.runtime.core.privileged.profiling.tracing.SpanCustomizer;

/**
 * A {@link SpanCustomizer} that sets the name of the span based on the component.
 *
 * @since 4.5.0
 */
public abstract class AbstractNamedSpanBasedOnComponentIdentifierSpanCustomizer
    extends AbstractDefaultAttributesResolvingSpanCustomizer {

  protected Component component;

  public AbstractNamedSpanBasedOnComponentIdentifierSpanCustomizer(Component component) {
    this.component = component;
  }

  @Override
  public ChildSpanInfo getChildSpanCustomizer() {
    return ChildSpanCustomizerResolver.getChildSpanCustomizer(component);
  }

  @Override
  public String getLocationAsString(CoreEvent coreEvent) {
    // The location is based only n the component.
    return CoreEventSpanUtils.getLocationAsString(component.getLocation());
  }

  /**
   * Resolves the {@link ChildSpanInfo} based on the {@link Component}'s identifier.
   */
  static class ChildSpanCustomizerResolver {

    public static final String UNTIL_SUCCESSFUL = "until-successful";

    public static final String SPAN_NAME_SEPARATOR = ":";

    public static ChildSpanInfo getChildSpanCustomizer(Component component) {
      if (component.getIdentifier().getName().equals(UNTIL_SUCCESSFUL)) {
        return new DefaultChildSpanInfo(SPAN_NAME_SEPARATOR + "attempt");
      }
      return new DefaultChildSpanInfo(SPAN_NAME_SEPARATOR + "route");
    }
  }
}
