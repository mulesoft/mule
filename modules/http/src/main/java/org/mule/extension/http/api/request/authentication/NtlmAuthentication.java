/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.request.authentication;

import static org.mule.runtime.module.http.internal.request.HttpAuthenticationType.NTLM;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.module.http.internal.domain.request.HttpRequestAuthentication;

/**
 * Configures NTLM authentication for the requests.
 *
 * @since 4.0
 */
public class NtlmAuthentication extends UsernamePasswordAuthentication {

  /**
   * The domain to authenticate.
   */
  @Parameter
  @Optional
  private String domain;

  /**
   * The workstation to authenticate.
   */
  @Parameter
  @Optional
  private String workstation;

  public String getDomain() {
    return domain;
  }

  public String getWorkstation() {
    return workstation;
  }

  @Override
  public HttpRequestAuthentication buildRequestAuthentication() {
    HttpRequestAuthentication requestAuthentication = getBaseRequestAuthentication(NTLM);
    requestAuthentication.setDomain(domain);
    requestAuthentication.setWorkstation(workstation);
    return requestAuthentication;
  }
}
