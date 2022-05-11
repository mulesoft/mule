/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.semantic.extension;

import org.mule.sdk.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.semantics.connectivity.ConfiguresNtlmProxy;
import org.mule.sdk.api.annotation.semantics.connectivity.NtlmDomain;

import java.util.Objects;

@ConfiguresNtlmProxy
public class NtlmProxyConfiguration {

  @Parameter
  @NtlmDomain
  private String ntlmDomain;

  public String getNtlmDomain() {
    return ntlmDomain;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NtlmProxyConfiguration that = (NtlmProxyConfiguration) o;
    return Objects.equals(ntlmDomain, that.ntlmDomain);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ntlmDomain);
  }
}
