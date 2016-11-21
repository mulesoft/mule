/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.request.validator;

import static org.mule.extension.http.internal.HttpConnectorConstants.API_CONFIGURATION;
import static org.mule.extension.http.internal.HttpConnectorConstants.OTHER_SETTINGS;
import static org.mule.extension.http.internal.HttpConnectorConstants.REQUEST_SETTINGS;
import static org.mule.extension.http.internal.HttpConnectorConstants.RESPONSE_SETTINGS;
import static org.mule.extension.http.internal.HttpConnectorConstants.URL_CONFIGURATION;
import static org.mule.runtime.extension.api.annotation.param.display.Placement.ADVANCED;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import org.mule.extension.http.api.HttpSendBodyMode;
import org.mule.extension.http.api.HttpStreamingType;
import org.mule.extension.http.api.request.RamlApiConfiguration;
import org.mule.extension.http.internal.request.HttpRequestOperations;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.net.CookieManager;
import java.util.function.Function;

import javax.inject.Inject;

/**
 * Configuration element for a HTTP requests.
 *
 * @since 4.0
 */
@Configuration(name = "request-config")
@ConnectionProviders(HttpRequesterProvider.class)
@Operations({HttpRequestOperations.class})
public class HttpRequesterConfig implements Initialisable {

  /**
   * Base path to use for all requests that reference this config.
   */
  @Parameter
  @Optional(defaultValue = "/")
  @Placement(group = URL_CONFIGURATION, order = 1)
  private Function<Event, String> basePath;

  /**
   * Specifies whether to follow redirects or not. Default value is true.
   */
  @Parameter
  @Optional(defaultValue = "true")
  @Placement(tab = ADVANCED, group = REQUEST_SETTINGS, order = 1)
  private Function<Event, Boolean> followRedirects;

  /**
   * Defines if the request should contain a body or not. If AUTO, it will depend on the method (GET, HEAD and OPTIONS will not
   * send a body).
   */
  @Parameter
  @Optional(defaultValue = "AUTO")
  @Placement(tab = ADVANCED, group = REQUEST_SETTINGS, order = 2)
  private Function<Event, HttpSendBodyMode> sendBodyMode;

  /**
   * Defines if the request should be sent using streaming or not. If this attribute is not present, the behavior will depend on
   * the type of the payload (it will stream only for InputStream). If set to true, it will always stream. If set to false, it
   * will never stream. As streaming is done the request will be sent user Transfer-Encoding: chunked.
   */
  @Parameter
  @Optional(defaultValue = "AUTO")
  @Placement(tab = ADVANCED, group = REQUEST_SETTINGS, order = 3)
  @Summary("Defines if the request should be sent using streaming or not. If this attribute is not present, "
      + "the behavior will depend on the type of the payload (it will stream only for InputStream).")
  private Function<Event, HttpStreamingType> requestStreamingMode;

  /**
   * By default, the response will be parsed (for example, a multipart response will be mapped as a Mule message with null payload
   * and inbound attachments with each part). If this property is set to false, no parsing will be done, and the payload will
   * always contain the raw contents of the HTTP response.
   */
  @Parameter
  @Optional(defaultValue = "true")
  @Placement(tab = ADVANCED, group = RESPONSE_SETTINGS, order = 1)
  @Summary("Indicates if the HTTP response should be parsed, or directly receive the raw content")
  private Function<Event, Boolean> parseResponse;

  /**
   * Maximum time that the request element will block the execution of the flow waiting for the HTTP response. If this value is
   * not present, the default response timeout from the Mule configuration will be used.
   */
  @Parameter
  @Optional
  @Placement(tab = ADVANCED, group = RESPONSE_SETTINGS, order = 2)
  private Function<Event, Integer> responseTimeout;

  /**
   * If true, cookies received in HTTP responses will be stored, and sent in subsequent HTTP requests.
   */
  @Parameter
  @Optional(defaultValue = "true")
  @Expression(NOT_SUPPORTED)
  @Placement(tab = ADVANCED, group = OTHER_SETTINGS)
  private boolean enableCookies;

  /**
   * Specifies a RAML configuration file for the API that is being consumed.
   */
  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  @Placement(group = API_CONFIGURATION)
  private RamlApiConfiguration apiConfiguration;

  @Inject
  private MuleContext muleContext;
  private CookieManager cookieManager;

  @Override
  public void initialise() throws InitialisationException {
    if (enableCookies) {
      cookieManager = new CookieManager();
    }
  }

  public Function<Event, String> getBasePath() {
    return basePath;
  }

  public Function<Event, Boolean> getFollowRedirects() {
    return followRedirects;
  }

  public Function<Event, Boolean> getParseResponse() {
    return parseResponse;
  }

  public Function<Event, HttpStreamingType> getRequestStreamingMode() {
    return requestStreamingMode;
  }

  public Function<Event, HttpSendBodyMode> getSendBodyMode() {
    return sendBodyMode;
  }

  public Function<Event, Integer> getResponseTimeout() {
    return responseTimeout;
  }

  public boolean isEnableCookies() {
    return enableCookies;
  }

  public RamlApiConfiguration getApiConfiguration() {
    return apiConfiguration;
  }

  public CookieManager getCookieManager() {
    return cookieManager;
  }

  public MuleContext getMuleContext() {
    return muleContext;
  }

}
