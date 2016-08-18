/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.http.api.client;

public class HttpRequestAuthentication {

  private final HttpAuthenticationType type;

  private String username;
  private String password;
  private String domain;
  private String workstation;
  private boolean preemptive;

  public HttpRequestAuthentication(HttpAuthenticationType type) {
    this.type = type;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public HttpAuthenticationType getType() {
    return type;
  }

  public String getWorkstation() {
    return workstation;
  }

  public void setWorkstation(String workstation) {
    this.workstation = workstation;
  }

  public boolean isPreemptive() {
    return preemptive;
  }

  public void setPreemptive(boolean preemptive) {
    this.preemptive = preemptive;
  }


}
