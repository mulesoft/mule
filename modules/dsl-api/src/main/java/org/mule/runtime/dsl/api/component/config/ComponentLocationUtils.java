/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.component.config;

import org.mule.runtime.api.component.location.ComponentLocation;

/**
 * Provides reusable utilities for {@link ComponentLocation} objects.
 * 
 * @since 4.0
 */
public final class ComponentLocationUtils {

  private ComponentLocationUtils() {}

  /**
   * Gets the name of the flow from a {@link ComponentLocation}.
   * 
   * @param location where to get the flowName from. Non-null.
   * @return the first part path of the provided {@code location}. Non-null.
   */
  public static String getFlowNameFrom(ComponentLocation location) {
    return location.getParts().get(0).getPartPath();
  }
}
