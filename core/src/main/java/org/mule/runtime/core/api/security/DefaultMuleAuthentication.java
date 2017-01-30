/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.security;

import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.api.security.Credentials;

import java.util.Map;

/**
 * Default implementation of {@link Authentication}
 *
 * @since 4.0
 */
public class DefaultMuleAuthentication implements Authentication {

  private boolean authenticated;
  private char[] credentials;
  private String user;
  private Map<String, Object> properties;

  public DefaultMuleAuthentication(Credentials credentials) {
    this.user = credentials.getUsername();
    this.credentials = credentials.getPassword();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setAuthenticated(boolean authenticated) {
    this.authenticated = authenticated;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isAuthenticated() {
    return authenticated;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getCredentials() {
    return new String(credentials);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getPrincipal() {
    return user;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, Object> getProperties() {
    return properties;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setProperties(Map<String, Object> properties) {
    this.properties = properties;
  }
}
