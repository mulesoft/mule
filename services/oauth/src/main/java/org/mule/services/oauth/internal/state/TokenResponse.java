/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.oauth.internal.state;

import static java.util.Collections.unmodifiableMap;

import java.util.HashMap;
import java.util.Map;

public class TokenResponse {

  private String accessToken;
  private String refreshToken;
  private String expiresIn;
  private Map<String, Object> customResponseParameters = new HashMap<>();

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = isEmpty(accessToken) ? null : accessToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = isEmpty(refreshToken) ? null : refreshToken;
  }

  public String getExpiresIn() {
    return expiresIn;
  }

  public void setExpiresIn(String expiresIn) {
    this.expiresIn = expiresIn;
  }

  public Map<String, Object> getCustomResponseParameters() {
    return unmodifiableMap(customResponseParameters);
  }

  public void setCustomResponseParameters(Map<String, Object> customResponseParameters) {
    this.customResponseParameters.putAll(customResponseParameters);
  }

  private boolean isEmpty(String value) {
    return value == null || org.mule.runtime.core.util.StringUtils.isEmpty(value) || "null".equals(value);
  }

}
