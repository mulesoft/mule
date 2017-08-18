/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.client.auth;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.http.api.client.auth.HttpAuthentication.HttpNtlmAuthentication;

/**
 * Default implementation of both {@link HttpAuthentication} and {@link HttpNtlmAuthentication}. Instances can only be obtained
 * through an {@link HttpAuthenticationBuilder}.
 *
 * @since 4.0
 */
final class DefaultHttpAuthentication implements HttpAuthentication, HttpNtlmAuthentication {

  private final HttpAuthenticationType type;
  private final String username;
  private final String password;
  private final boolean preemptive;

  private final String domain;
  private final String workstation;

  DefaultHttpAuthentication(HttpAuthenticationType type, String username, String password, boolean preemptive, String domain,
                            String workstation) {
    checkArgument(type != null, "An authentication type must be declared.");
    checkArgument(username != null, "A username must be provided.");
    checkArgument(password != null, "A password must be provided.");
    this.type = type;
    this.username = username;
    this.password = password;
    this.preemptive = preemptive;
    this.domain = domain;
    this.workstation = workstation;
  }

  @Override
  public HttpAuthenticationType getType() {
    return type;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public boolean isPreemptive() {
    return preemptive;
  }

  @Override
  public String getDomain() {
    return domain;
  }

  @Override
  public String getWorkstation() {
    return workstation;
  }

}
