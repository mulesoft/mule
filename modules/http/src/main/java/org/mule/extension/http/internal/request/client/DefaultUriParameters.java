/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.request.client;

import org.mule.extension.http.api.request.client.UriParameters;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.module.http.api.HttpConstants.Protocols;

import java.util.function.Function;

/**
 * Default implementation of {@link UriParameters}.
 *
 * @since 4.0
 */
public class DefaultUriParameters implements UriParameters {

  private final Protocols scheme;
  private final Function<Event, String> host;
  private final Function<Event, Integer> port;


  public DefaultUriParameters(Protocols protocol, Function<Event, String> host, Function<Event, Integer> port) {
    this.scheme = protocol;
    this.host = host;
    this.port = port;
  }

  @Override
  public Protocols getScheme() {
    return scheme;
  }

  @Override
  public Function<Event, String> getHost() {
    return host;
  }

  @Override
  public Function<Event, Integer> getPort() {
    return port;
  }
}
