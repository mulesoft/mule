/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.client.auth;

import org.mule.runtime.http.api.client.auth.HttpAuthentication.HttpNtlmAuthentication;
import org.mule.runtime.http.api.client.auth.HttpAuthenticationBuilder.HttpNtlmAuthenticationBuilder;

/**
 * Default implementation of both {@link HttpAuthenticationBuilder} and {@link HttpNtlmAuthenticationBuilder}. Instances can only
 * be obtained using {@link HttpAuthentication#builder()}, {@link HttpNtlmAuthentication#builder()} or their helper methods.
 *
 * @since 4.0
 */
final class DefaultHttpAuthenticationBuilder implements HttpAuthenticationBuilder,
    HttpNtlmAuthenticationBuilder {

  private HttpAuthenticationType type;
  private String username;
  private String password;
  private boolean preemptive = true;
  private String domain;
  private String workstation;

  DefaultHttpAuthenticationBuilder() {

  }

  @Override
  public HttpNtlmAuthenticationBuilder type(HttpAuthenticationType type) {
    this.type = type;
    return this;
  }

  @Override
  public HttpNtlmAuthenticationBuilder username(String username) {
    this.username = username;
    return this;
  }

  @Override
  public HttpNtlmAuthenticationBuilder password(String password) {
    this.password = password;
    return this;
  }

  @Override
  public HttpNtlmAuthenticationBuilder preemptive(boolean preemptive) {
    this.preemptive = preemptive;
    return this;
  }

  @Override
  public HttpNtlmAuthenticationBuilder domain(String domain) {
    this.domain = domain;
    return this;
  }

  @Override
  public HttpNtlmAuthenticationBuilder workstation(String workstation) {
    this.workstation = workstation;
    return this;
  }

  @Override
  public HttpNtlmAuthentication build() {
    return new DefaultHttpAuthentication(type, username, password, preemptive, domain, workstation);
  }

}
