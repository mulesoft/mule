/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
