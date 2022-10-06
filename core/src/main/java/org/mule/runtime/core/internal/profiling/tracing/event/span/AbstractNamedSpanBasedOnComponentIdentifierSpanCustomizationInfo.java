/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.tracing.event.span;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.profiling.tracing.ChildSpanCustomizationInfo;
import org.mule.runtime.core.privileged.profiling.tracing.SpanCustomizationInfo;

/**
 * A {@link SpanCustomizationInfo} that sets the name of the span based on the component.
 *
 * @since 4.5.0
 */
public abstract class AbstractNamedSpanBasedOnComponentIdentifierSpanCustomizationInfo
    extends AbstractDefaultAttributesResolvingSpanCustomizationInfo {

  public static final String SPAN_NAME_SEPARATOR = ":";
  public static final String ROUTE_TAG = "route";

  protected Component component;

  public AbstractNamedSpanBasedOnComponentIdentifierSpanCustomizationInfo(Component component) {
    this.component = component;
  }

  @Override
  public ChildSpanCustomizationInfo getChildSpanCustomizationInfo() {
    return ChildSpanCustomizationInfoResolver.getChildSpanCustomizationInfo();
  }

  @Override
  public String getLocationAsString(CoreEvent coreEvent) {
    // The location is based only n the component.
    return CoreEventSpanUtils.getLocationAsString(component.getLocation());
  }

  /**
   * Resolves the {@link ChildSpanCustomizationInfo} based on the {@link Component}'s identifier.
   */
  static class ChildSpanCustomizationInfoResolver {

    public static ChildSpanCustomizationInfo getChildSpanCustomizationInfo() {
      return new DefaultChildSpanCustomizationInfo(SPAN_NAME_SEPARATOR + ROUTE_TAG);
    }
  }
}
