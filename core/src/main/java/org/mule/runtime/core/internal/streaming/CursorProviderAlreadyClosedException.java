/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static org.mule.runtime.api.util.MuleSystemProperties.TRACK_CURSOR_PROVIDER_CLOSE_PROPERTY;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.streaming.CursorProvider;

/**
 * This {@link RuntimeException} is thrown when it is trying to open a closed {@link CursorProvider}. It can contain optionally
 * the {@link ComponentLocation} where the Cursor was created.
 *
 * @since 4.3.0
 */
public class CursorProviderAlreadyClosedException extends RuntimeException {

  private final Optional<ComponentLocation> originatingLocation;

  public CursorProviderAlreadyClosedException(String message) {
    this(message, empty());
  }

  public CursorProviderAlreadyClosedException(String message, Optional<ComponentLocation> originatingLocation) {
    this(message, originatingLocation, empty());
  }

  public CursorProviderAlreadyClosedException(String message, Optional<ComponentLocation> originatingLocation,
                                              Optional<Exception> closerResponsible) {
    super(resolveMessage(message, originatingLocation, closerResponsible));

    this.originatingLocation = originatingLocation;
  }

  public Optional<ComponentLocation> getOriginatingLocation() {
    return originatingLocation;
  }

  private static String resolveMessage(String message, Optional<ComponentLocation> originatingLocation,
                                       Optional<Exception> closerResponsible) {
    String openedByDescription = format("The cursor provider was open by %s", originatingLocation.map(cl -> cl.getLocation())
        .orElse("unknown"));

    String responsibleDescription = closerResponsible.map(r -> {
      StringWriter stringWriter = new StringWriter();
      r.printStackTrace(new PrintWriter(stringWriter));
      return format("The cursor provider was closed by: \n %s", stringWriter.toString());
    }).orElse(format("Set SystemProperty '%s' in true for more details", TRACK_CURSOR_PROVIDER_CLOSE_PROPERTY));

    return format("%s: %s. %s.", message, openedByDescription, responsibleDescription);
  }

}
