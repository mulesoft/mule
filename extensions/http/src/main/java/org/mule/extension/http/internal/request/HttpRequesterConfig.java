/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.request;

import static org.mule.extension.http.internal.HttpConnectorConstants.API_SPECIFICATION;
import static org.mule.extension.http.internal.HttpConnectorConstants.OTHER_SETTINGS;
import static org.mule.extension.http.internal.HttpConnectorConstants.REQUEST_SETTINGS;
import static org.mule.extension.http.internal.HttpConnectorConstants.RESPONSE_SETTINGS;
import static org.mule.extension.http.internal.HttpConnectorConstants.URL_CONFIGURATION;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;

import org.mule.extension.http.api.request.RamlApiConfiguration;
import org.mule.extension.http.internal.HttpStreamingType;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.Placement;

import java.net.CookieManager;

import javax.inject.Inject;

/**
 * Configuration element for a HTTP requests.
 *
 * @since 4.0
 */
@Configuration(name = "request-config")
@ConnectionProviders(HttpRequesterProvider.class)
@Operations({HttpRequestOperations.class})
public class HttpRequesterConfig implements Initialisable, HttpRequesterCookieConfig {

  @ParameterGroup(name = URL_CONFIGURATION)
  @NullSafe
  private RequestUrlConfiguration urlConfiguration;

  @ParameterGroup(name = REQUEST_SETTINGS)
  @NullSafe
  private RequestSettings requestSettings;

  @ParameterGroup(name = RESPONSE_SETTINGS)
  @Placement(tab = RESPONSE_SETTINGS)
  @NullSafe
  private ResponseSettings responseSettings;

  @ParameterGroup(name = OTHER_SETTINGS)
  @NullSafe
  private OtherRequestSettings otherSettings;

  /**
   * Specifies a RAML configuration file for the I that is being consumed.
   */
  @Parameter
  @Optional
  @Expression(NOT_SUPPORTED)
  @Placement(tab = API_SPECIFICATION)
  private RamlApiConfiguration apiConfiguration;

  @Inject
  private MuleContext muleContext;
  private CookieManager cookieManager;

  @Override
  public void initialise() throws InitialisationException {
    if (otherSettings.isEnableCookies()) {
      cookieManager = new CookieManager();
    }
  }

  public String getBasePath() {
    return urlConfiguration.getBasePath();
  }

  public Boolean getFollowRedirects() {
    return requestSettings.getFollowRedirects();
  }

  public Boolean getParseResponse() {
    return responseSettings.getParseResponse();
  }

  public HttpStreamingType getRequestStreamingMode() {
    return requestSettings.getRequestStreamingMode();
  }

  public HttpSendBodyMode getSendBodyMode() {
    return requestSettings.getSendBodyMode();
  }

  public Integer getResponseTimeout() {
    return responseSettings.getResponseTimeout();
  }

  @Override
  public boolean isEnableCookies() {
    return otherSettings.isEnableCookies();
  }

  public RamlApiConfiguration getApiConfiguration() {
    return apiConfiguration;
  }

  @Override
  public CookieManager getCookieManager() {
    return cookieManager;
  }

  public MuleContext getMuleContext() {
    return muleContext;
  }

}
