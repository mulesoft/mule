/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api.state;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.lock.LockFactory;

import java.util.Map;
import java.util.concurrent.locks.Lock;

/**
 * OAuth state for a particular resource owner, which typically represents an user.
 *
 * @since 4.0
 */
@NoImplement
public interface ResourceOwnerOAuthContext {

  String DEFAULT_RESOURCE_OWNER_ID = "default";

  /**
   * @return access token of the oauth context retrieved by the token request
   */
  String getAccessToken();

  /**
   * @return refresh token of the oauth context retrieved by the token request
   */
  String getRefreshToken();

  /**
   * @return state of the oauth context send in the authorization request
   */
  String getState();

  /**
   * @return expires in value retrieved by the token request.
   */
  String getExpiresIn();

  /**
   * @return custom token request response parameters configured for extraction.
   */
  Map<String, Object> getTokenResponseParameters();

  /**
   * @return id for the oauth state.
   */
  String getResourceOwnerId();

  /**
   * @return the state of the dance for the resource owner of this context.
   */
  DancerState getDancerState();

  /**
   * Updates the state of the dancer for this context.
   * <p>
   * Note that calling this just updates the internal state of this object. Calling this method does not persist this change in
   * any store. That has to be done explicitly after calling this method.
   *
   * @param dancerState the state of the dance for the resource owner of this context.
   */
  void setDancerState(DancerState dancerState);

  /**
   * Obtains a lock to avoid triggering a refresh of the context more than once simultaneously.
   *
   * @param lockNamePrefix a prefix to uniquely identify the locks in the provided {@code lockFactory}.
   * @param lockFactory the object to get the locks from. This will ensure that the same lock instance is used after
   *        serialization/deserialization of this context.
   * @return a lock that can be used to avoid concurrency problems trying to update oauth context.
   */
  Lock getRefreshOAuthContextLock(String lockNamePrefix, LockFactory lockFactory);

}
