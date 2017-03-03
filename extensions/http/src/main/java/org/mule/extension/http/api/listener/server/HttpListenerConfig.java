/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.listener.server;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED_TAB;

import org.mule.extension.http.internal.listener.HttpListener;
import org.mule.extension.http.internal.listener.HttpListenerProvider;
import org.mule.extension.http.internal.listener.ListenerPath;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

/**
 * Configuration element for a {@link HttpListener}.
 *
 * @since 4.0
 */
@Configuration(name = "listener-config")
@ConnectionProviders(HttpListenerProvider.class)
@Sources(HttpListener.class)
public class HttpListenerConfig implements Initialisable {

  /**
   * Base path to use for all requests that reference this config.
   */
  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  private String basePath;

  /**
   * By default, the request will be parsed (for example, a multi part request will be mapped as a Mule message with no payload
   * and attributes with each part). If this property is set to false, no parsing will be done, and the payload will always
   * contain the raw contents of the HTTP request.
   */
  @Parameter
  @Optional(defaultValue = "true")
  @Expression(NOT_SUPPORTED)
  @Placement(tab = ADVANCED_TAB)
  private Boolean parseRequest;

  /**
   * Ideal for proxy scenarios, this indicates whether errors produced by an HTTP request should be interpreted by the listener.
   * If enabled, an error thrown by an HTTP request operation reaching a listener will be analysed for response data, so if a
   * request operation throws a FORBIDDEN error, for example, then the listener will generate a 403 error response.
   */
  @Parameter
  @Optional(defaultValue = "false")
  @Expression(NOT_SUPPORTED)
  @Placement(tab = ADVANCED_TAB)
  private Boolean interpretRequestErrors;


  @Override
  public void initialise() throws InitialisationException {
    basePath = sanitizePathWithStartSlash(this.basePath);
  }

  public ListenerPath getFullListenerPath(String listenerPath) {
    checkArgument(listenerPath.startsWith("/"), "listenerPath must start with /");
    return new ListenerPath(basePath, listenerPath);
  }

  public Boolean resolveParseRequest(Boolean listenerParseRequest) {
    return listenerParseRequest != null ? listenerParseRequest : parseRequest;
  }

  public Boolean resolveInterpretRequestErrors(Boolean listenerInterpretRequestErrors) {
    return listenerInterpretRequestErrors != null ? listenerInterpretRequestErrors : interpretRequestErrors;
  }

  public String sanitizePathWithStartSlash(String path) {
    if (path == null) {
      return null;
    }
    return path.startsWith("/") ? path : "/" + path;
  }

}
