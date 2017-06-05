/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api.state;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;

/**
 * OAuth state for a particular resource owner which typically represents an user.
 * 
 * @since 4.0, was ResourceOwnerOAuthContext in previous versions
 */
public final class DefaultResourceOwnerOAuthContext implements ResourceOwnerOAuthContext, Serializable {

  private static final long serialVersionUID = -4260965520423792113L;

  private final String resourceOwnerId;
  private transient Lock refreshUserOAuthContextLock;
  private String accessToken;
  private String refreshToken;
  private String state;
  private String expiresIn;
  private Map<String, Object> tokenResponseParameters = new HashMap<String, Object>();

  public DefaultResourceOwnerOAuthContext(final Lock refreshUserOAuthContextLock, final String resourceOwnerId) {
    this.refreshUserOAuthContextLock = refreshUserOAuthContextLock;
    this.resourceOwnerId = resourceOwnerId;
  }

  @Override
  public String getAccessToken() {
    return accessToken;
  }

  @Override
  public String getRefreshToken() {
    return refreshToken;
  }

  @Override
  public String getState() {
    return state;
  }

  public void setAccessToken(final String accessToken) {
    this.accessToken = accessToken;
  }

  public void setRefreshToken(final String refreshToken) {
    this.refreshToken = refreshToken;
  }

  public void setExpiresIn(final String expiresIn) {
    this.expiresIn = expiresIn;
  }

  @Override
  public String getExpiresIn() {
    return expiresIn;
  }

  public void setState(final String state) {
    this.state = state;
  }

  @Override
  public Map<String, Object> getTokenResponseParameters() {
    return tokenResponseParameters;
  }

  public void setTokenResponseParameters(final Map<String, Object> tokenResponseParameters) {
    this.tokenResponseParameters = tokenResponseParameters;
  }

  /**
   * @return a lock that can be used to avoid concurrency problems trying to update oauth context.
   */
  public Lock getRefreshUserOAuthContextLock() {
    return refreshUserOAuthContextLock;
  }

  @Override
  public String getResourceOwnerId() {
    return resourceOwnerId == null ? DEFAULT_RESOURCE_OWNER_ID : resourceOwnerId;
  }

  public void setRefreshUserOAuthContextLock(Lock refreshUserOAuthContextLock) {
    this.refreshUserOAuthContextLock = refreshUserOAuthContextLock;
  }
}
