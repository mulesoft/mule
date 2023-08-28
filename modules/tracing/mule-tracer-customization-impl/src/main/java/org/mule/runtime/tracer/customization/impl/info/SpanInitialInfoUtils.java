/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.customization.impl.info;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;

public class SpanInitialInfoUtils {

  private static final String CORE_EVENT_SPAN_NAME_SEPARATOR = ":";
  public static final String UNKNOWN = "unknown";

  /**
   * @param componentIdentifier the {@link ComponentIdentifier}
   * @return the name for the {@link org.mule.runtime.api.profiling.tracing.Span}
   */
  public static String getSpanName(ComponentIdentifier componentIdentifier) {
    return getUnknownIfEmptyNamespace(componentIdentifier) + CORE_EVENT_SPAN_NAME_SEPARATOR
        + getUnknownIfEmptyName(componentIdentifier);
  }

  private static String getUnknownIfEmptyName(ComponentIdentifier componentIdentifier) {
    if (componentIdentifier == null) {
      return UNKNOWN;
    }

    return componentIdentifier.getName();
  }

  private static String getUnknownIfEmptyNamespace(ComponentIdentifier componentIdentifier) {
    if (componentIdentifier == null) {
      return UNKNOWN;
    }

    return componentIdentifier.getNamespace();
  }

  /**
   * @param componentLocation the component location
   * @return componentLocation as string or unknown
   */
  public static String getLocationAsString(ComponentLocation componentLocation) {
    if (componentLocation != null) {
      return componentLocation.getLocation();
    }

    return UNKNOWN;
  }

}
