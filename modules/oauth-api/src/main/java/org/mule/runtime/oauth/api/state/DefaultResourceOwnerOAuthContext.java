/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api.state;

import static org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext.DancerState.HAS_TOKEN;
import static org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext.DancerState.NO_TOKEN;

import org.mule.runtime.api.lock.LockFactory;

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

  private static final long serialVersionUID = 6118607567823801246L;

  private final String resourceOwnerId;
  private DancerState dancerState = NO_TOKEN;
  private transient Lock refreshUserOAuthContextLock;
  private String accessToken;
  private String refreshToken;
  private String state;
  private String expiresIn;
  private Map<String, Object> tokenResponseParameters = new HashMap<>();

  public DefaultResourceOwnerOAuthContext(final String resourceOwnerId) {
    this.resourceOwnerId = resourceOwnerId;
  }

  /**
   * @deprecated use {@link #getRefreshOAuthContextLock(String, LockFactory)} instead of the lock provided here
   */
  @Deprecated
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
   * @deprecated use {@link #getRefreshOAuthContextLock(String, LockFactory)} instead of the lock provided here
   */
  @Deprecated
  public Lock getRefreshUserOAuthContextLock() {
    return refreshUserOAuthContextLock;
  }

  @Override
  public String getResourceOwnerId() {
    return resourceOwnerId == null ? DEFAULT_RESOURCE_OWNER_ID : resourceOwnerId;
  }

  /**
   * @deprecated use {@link #getRefreshOAuthContextLock(String, LockFactory)} instead of the lock provided here
   */
  @Deprecated
  public void setRefreshUserOAuthContextLock(Lock refreshUserOAuthContextLock) {
    this.refreshUserOAuthContextLock = refreshUserOAuthContextLock;
  }

  @Override
  public DancerState getDancerState() {
    // By default, new instances are built with NO_TOKEN.
    // This condition here accounts for the case where the object was generated by a previous version where this attribute wasn't
    // there.
    // In that case, the equivalent state for that is HAS_TOKEN.
    return dancerState != null ? dancerState : HAS_TOKEN;
  }

  public void setDancerState(DancerState dancerState) {
    this.dancerState = dancerState;
  }

  @Override
  public Lock getRefreshOAuthContextLock(String lockNamePrefix, LockFactory lockProvider) {
    return createRefreshOAuthContextLock(lockNamePrefix, lockProvider, resourceOwnerId);
  }

  /**
   * This utility method is needed for the cases where the context is being queried. There is still no context created, but the
   * same lock that this context would use is required for querying thread-safely.
   */
  public static Lock createRefreshOAuthContextLock(String lockNamePrefix, LockFactory lockProvider, String resourceOwnerId) {
    return lockProvider.createLock(lockNamePrefix + "_oauth:" + resourceOwnerId);
  }
}
