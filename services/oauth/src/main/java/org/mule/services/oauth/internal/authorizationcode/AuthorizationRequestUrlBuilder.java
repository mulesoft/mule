/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.oauth.internal.authorizationcode;

import static java.lang.String.format;
import static java.net.URLEncoder.encode;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.services.oauth.internal.OAuthConstants.CLIENT_ID_PARAMETER;
import static org.mule.services.oauth.internal.OAuthConstants.REDIRECT_URI_PARAMETER;
import static org.mule.services.oauth.internal.OAuthConstants.SCOPE_PARAMETER;
import static org.mule.services.oauth.internal.OAuthConstants.STATE_PARAMETER;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

/**
 * Builds the authorization url to redirect the user to.
 */
public class AuthorizationRequestUrlBuilder {

  private static final Logger LOGGER = getLogger(AuthorizationRequestUrlBuilder.class);

  private static final String ADDED_PARAMETER_TEMPLATE = "&%s=";

  private String authorizationUrl;
  private String redirectUrl;
  private String clientId;
  private String scope;
  private String clientSecret;
  private Map<String, String> customParameters = new HashMap<>();
  private String state;

  private Charset encoding;

  public AuthorizationRequestUrlBuilder setAuthorizationUrl(String authorizationUrl) {
    this.authorizationUrl = authorizationUrl;
    return this;
  }

  public AuthorizationRequestUrlBuilder setRedirectUrl(String redirectUrl) {
    this.redirectUrl = redirectUrl;
    return this;
  }

  public AuthorizationRequestUrlBuilder setClientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

  public AuthorizationRequestUrlBuilder setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
    return this;
  }

  public AuthorizationRequestUrlBuilder setScope(String scope) {
    this.scope = scope;
    return this;
  }

  public AuthorizationRequestUrlBuilder setCustomParameters(Map<String, String> customParameters) {
    this.customParameters = customParameters;
    return this;
  }

  public AuthorizationRequestUrlBuilder setEncoding(Charset encoding) {
    this.encoding = encoding;
    return this;
  }

  /**
   * @return the authorization url with all the query parameters from the config.
   */
  public String buildUrl() {
    checkArgument(isNotBlank(clientId), "clientId cannot be blank");
    checkArgument(isNotBlank(clientSecret), "clientSecret cannot be blank");
    checkArgument(isNotBlank(authorizationUrl), "authorizationUrl cannot be blank");
    checkArgument(customParameters != null, "customParameters cannot be null");
    return buildAuthorizeUrl();
  }

  private final String buildAuthorizeUrl() {
    StringBuilder urlBuilder = new StringBuilder();
    urlBuilder.append(authorizationUrl);

    urlBuilder.append("?").append("response_type=code&").append(CLIENT_ID_PARAMETER + "=").append(clientId);

    try {
      if (isNotBlank(scope)) {
        urlBuilder.append(format(ADDED_PARAMETER_TEMPLATE, SCOPE_PARAMETER))
            .append(encode(scope, encoding.name()));
      }
      if (isNotBlank(state)) {
        urlBuilder.append(format(ADDED_PARAMETER_TEMPLATE, STATE_PARAMETER))
            .append(encode(state, encoding.name()));
      }

      for (Map.Entry<String, String> entry : customParameters.entrySet()) {
        urlBuilder.append("&").append(entry.getKey()).append("=").append(encode(entry.getValue(), encoding.name()));
      }

      urlBuilder.append(format(ADDED_PARAMETER_TEMPLATE, REDIRECT_URI_PARAMETER))
          .append(encode(redirectUrl, encoding.name()));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(("Authorization URL has been generated as follows: " + urlBuilder));
    }
    return urlBuilder.toString();
  }

  public AuthorizationRequestUrlBuilder setState(String state) {
    this.state = state;
    return this;
  }

}
