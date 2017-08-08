/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.client.auth;

import static org.mule.runtime.http.api.client.auth.HttpAuthenticationType.BASIC;
import static org.mule.runtime.http.api.client.auth.HttpAuthenticationType.DIGEST;
import static org.mule.runtime.http.api.client.auth.HttpAuthenticationType.NTLM;
import org.mule.runtime.http.api.client.auth.HttpAuthenticationBuilder.HttpNtlmAuthenticationBuilder;

/**
 * Represents an HTTP request's username/password based authentication.
 *
 * @since 4.0
 */
public interface HttpAuthentication {

  /**
   * Provides a {@link HttpAuthenticationBuilder} to create instances of {@link HttpAuthentication}.
   *
   * @return a fresh builder
   */
  static HttpAuthenticationBuilder builder() {
    return new DefaultHttpAuthenticationBuilder();
  }

  /**
   * Provides a {@link HttpAuthenticationBuilder} already set up for basic authentication.
   *
   * @param username the username desired
   * @param password the password desired
   * @return a builder pre-configured for basic authentication
   */
  static HttpAuthenticationBuilder basic(String username, String password) {
    return new DefaultHttpAuthenticationBuilder().type(BASIC).username(username).password(password);
  }

  /**
   * Provides a {@link HttpAuthenticationBuilder} already set up for digest authentication.
   *
   * @param username the username desired
   * @param password the password desired
   * @return a builder pre-configured for digest authentication
   */
  static HttpAuthenticationBuilder digest(String username, String password) {
    return new DefaultHttpAuthenticationBuilder().type(DIGEST).username(username).password(password);
  }

  /**
   * Provides a {@link HttpNtlmAuthenticationBuilder} to create instances of {@link HttpNtlmAuthentication}.
   *
   * @param username the username desired
   * @param password the password desired
   * @return a builder pre-configured for NTLM authentication
   */
  static HttpNtlmAuthenticationBuilder ntlm(String username, String password) {
    return HttpNtlmAuthentication.builder().username(username).password(password);
  }

  /**
   * The type of authentication defined.
   *
   * @return the {@link HttpAuthenticationType}
   */
  HttpAuthenticationType getType();

  /**
   * The username defined.
   *
   * @return the username
   */
  String getUsername();

  /**
   * The password defined.
   *
   * @return the password
   */
  String getPassword();

  /**
   * Whether or not the authentication should be done from the first request. If {@code false} the request will be done awaiting a
   * 401 response with authentication details to start the process.
   *
   * @return whether the authentication should be preemptive or not
   */
  boolean isPreemptive();

  /**
   * Represents an HTTP request's NTLM based authentication.
   *
   * @since 4.0
   */
  interface HttpNtlmAuthentication extends HttpAuthentication {

    /**
     * Provides a {@link HttpNtlmAuthenticationBuilder} to create instances of {@link HttpNtlmAuthentication}.
     *
     * @return
     */
    static HttpNtlmAuthenticationBuilder builder() {
      return new DefaultHttpAuthenticationBuilder().type(NTLM);
    }

    /**
     * The domain defined.
     *
     * @return the domain
     */
    String getDomain();

    /**
     * The workstation defined.
     *
     * @return the workstation
     */
    String getWorkstation();

  }
}
