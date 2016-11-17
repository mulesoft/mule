/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.listener.server;

import static org.mule.extension.http.internal.HttpConnectorConstants.REQUEST_SETTINGS;
import static org.mule.extension.http.internal.HttpConnectorConstants.URL_CONFIGURATION;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import org.mule.extension.http.internal.listener.HttpListener;
import org.mule.extension.http.internal.listener.HttpListenerProvider;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.module.http.internal.HttpParser;
import org.mule.runtime.module.http.internal.listener.ListenerPath;

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
  @Placement(group = URL_CONFIGURATION)
  private String basePath;

  /**
   * By default, the request will be parsed (for example, a multi part request will be mapped as a Mule message with no payload
   * and attributes with each part). If this property is set to false, no parsing will be done, and the payload will always
   * contain the raw contents of the HTTP request.
   */
  @Parameter
  @Optional(defaultValue = "true")
  @Expression(NOT_SUPPORTED)
  @Placement(tab = ADVANCED, group = REQUEST_SETTINGS)
  private Boolean parseRequest;


  @Override
  public void initialise() throws InitialisationException {
    basePath = HttpParser.sanitizePathWithStartSlash(this.basePath);
  }

  public ListenerPath getFullListenerPath(String listenerPath) {
    checkArgument(listenerPath.startsWith("/"), "listenerPath must start with /");
    return new ListenerPath(basePath, listenerPath);
  }

  public Boolean resolveParseRequest(Boolean listenerParseRequest) {
    return listenerParseRequest != null ? listenerParseRequest : (parseRequest != null ? parseRequest : true);
  }
}
