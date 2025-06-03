/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.http.client;

import org.mule.runtime.http.api.client.auth.HttpAuthentication;
import org.mule.sdk.api.http.client.auth.HttpAuthenticationConfig;

public class HttpAuthenticationConfigurerImpl implements HttpAuthenticationConfig {

  private HttpAuthentication result;

  @Override
  public void basic(String username, String password, boolean preemptive) {
    result = HttpAuthentication.basic(username, password).preemptive(preemptive).build();
  }

  @Override
  public void digest(String username, String password, boolean preemptive) {
    result = HttpAuthentication.digest(username, password).preemptive(preemptive).build();
  }

  @Override
  public void ntlm(String username, String password, boolean preemptive, String domain, String workstation) {
    result = HttpAuthentication.ntlm(username, password).preemptive(preemptive).domain(domain).workstation(workstation).build();
  }

  public HttpAuthentication build() {
    return result;
  }
}
