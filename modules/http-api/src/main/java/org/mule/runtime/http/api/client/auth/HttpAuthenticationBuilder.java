/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.client.auth;

import org.mule.runtime.http.api.client.auth.HttpAuthentication.HttpNtlmAuthentication;

/**
 * Allows the creation of {@link HttpAuthentication} instances. At the very least, a type, username and password must be provided.
 *
 * @since 4.0
 */
public interface HttpAuthenticationBuilder {

  /**
   * Defines the {@link HttpAuthenticationType} to use. Must be provided.
   *
   * @param type the authentication scheme desired
   * @return this builder
   */
  HttpAuthenticationBuilder type(HttpAuthenticationType type);

  /**
   * Defines the username to use. Must be provided.
   *
   * @param username for the authentication
   * @return this builder
   */
  HttpAuthenticationBuilder username(String username);

  /**
   * Defines the password to use. Must be provided.
   *
   * @param password for the authentication
   * @return this builder
   */
  HttpAuthenticationBuilder password(String password);

  /**
   * Defines whether or not the authentication should be done from the first request. Default value is {@code true}, if {@code false}
   * the request will be done awaiting a 401 response with authentication details to start the process.
   *
   * @param preemptive whether or not the authentication process should be started at once
   * @return this builder
   */
  HttpAuthenticationBuilder preemptive(boolean preemptive);

  /**
   * Creates the {@link HttpAuthentication} as configured.
   *
   * @return a fresh authentication configuration
   */
  HttpAuthentication build();

  /**
   * Specification of {@link HttpAuthenticationBuilder} for NTLM based authentication.
   *
   * @since 4.0
   */
  interface HttpNtlmAuthenticationBuilder extends HttpAuthenticationBuilder {

    @Override
    HttpNtlmAuthenticationBuilder type(HttpAuthenticationType type);

    @Override
    HttpNtlmAuthenticationBuilder username(String username);

    @Override
    HttpNtlmAuthenticationBuilder password(String password);

    @Override
    HttpNtlmAuthenticationBuilder preemptive(boolean preemptive);

    /**
     * Defines the user domain to use.
     *
     * @param domain for the authentication
     * @return this builder
     */
    HttpNtlmAuthenticationBuilder domain(String domain);

    /**
     * Defines the user workstation to use.
     *
     * @param workstation for the authentication
     * @return this builder
     */
    HttpNtlmAuthenticationBuilder workstation(String workstation);

    @Override
    HttpNtlmAuthentication build();
  }

}
