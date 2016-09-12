/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.request.client;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.module.http.api.HttpConstants.Protocols;

import java.util.function.Function;

/**
 * Represents the default URI parameters an {@HttpClient} will receive in {@HttpRequest}s.
 *
 * @since 4.0
 */
public interface UriParameters {

  /**
   * @return the HTTP scheme for the URI.
   */
  Protocols getScheme();

  /**
   * @return the function that will resolve to the URI host.
   */
  Function<Event, String> getHost();

  /**
   * @return the function that will resolve to the URI port.
   */
  Function<Event, Integer> getPort();
}
