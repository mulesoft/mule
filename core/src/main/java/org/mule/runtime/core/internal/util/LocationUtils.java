/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static org.mule.runtime.api.component.location.Location.CONNECTION;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;

import org.mule.runtime.api.component.location.Location;

import java.util.List;

/**
 * Utility class for handling {@link Location} instances
 *
 * @since 4.4.0
 */
public class LocationUtils {

  /**
   * Utility method that given a {@link Location} creates a new one but ignoring the last part of it.
   */
  public static Location deleteLastPartFromLocation(Location location) {
    Location.Builder builder = Location.builder();
    List<String> parts = location.getParts();
    builder = builder.globalName(location.getGlobalName());
    for (int i = 0; i < parts.size() - 1; i++) {
      builder = builder.addPart(parts.get(i));
    }
    return builder.build();
  }

  /**
   * Given a string representation of a location, creates the location for the global element.
   */
  public static Location globalLocation(String location) {
    Location.Builder builder = Location.builder();
    builder = builder.globalName(builderFromStringRepresentation(location).build().getGlobalName());
    return builder.build();
  }

  /**
   * Introspect a {@link Location} and indicates if this one is pointing or not to a Connection
   *
   * @param location The given {@link Location} to introspect
   * @return a boolean indicating if the {@link Location} is pointing or not to a Connection
   */
  public static boolean isConnection(Location location) {
    return !location.getParts().isEmpty() && location.getParts().get(location.getParts().size() - 1).equals(CONNECTION);
  }
}
