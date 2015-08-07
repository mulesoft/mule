/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request;

/**
 * A Proxy configuration for NTLM authentication proxies.
 */
public class NtlmProxyConfig extends DefaultProxyConfig
{
    private String ntlmDomain;

    public String getNtlmDomain() { return ntlmDomain; }

    public void setNtlmDomain(String ntlmDomain) { this.ntlmDomain = ntlmDomain; }

}
