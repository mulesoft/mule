/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.request.proxy;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

/**
 * A Proxy configuration for NTLM authentication proxies.
 *
 * @since 4.0
 */
@Alias("ntlm-proxy")
public class NtlmProxyConfig extends DefaultProxyConfig {

  /**
   * The domain to authenticate against the proxy.
   */
  @Parameter
  @DisplayName("NTLM Domain")
  private String ntlmDomain;

  public String getNtlmDomain() {
    return ntlmDomain;
  }

  public void setNtlmDomain(String ntlmDomain) {
    this.ntlmDomain = ntlmDomain;
  }

}
