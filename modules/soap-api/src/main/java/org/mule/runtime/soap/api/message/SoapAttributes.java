/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.soap.api.message;

import static com.google.common.collect.ImmutableMap.copyOf;
import static com.google.common.collect.ImmutableMap.of;

import com.google.common.collect.ImmutableMap;

import java.io.Serializable;
import java.util.Map;

/**
 * Contains the headers retrieved by the protocol after the request and the specific outbound SOAP headers retrieved
 * after performing a web service operation.
 *
 * @since 4.0
 */
public final class SoapAttributes implements Serializable {

  private final Map<String, String> protocolHeaders;
  private final Map<String, String> soapHeaders;

  public SoapAttributes(Map<String, String> soapHeaders, Map<String, String> protocolHeaders) {
    this.soapHeaders = soapHeaders != null ? ImmutableMap.copyOf(soapHeaders) : ImmutableMap.of();
    this.protocolHeaders = protocolHeaders != null ? copyOf(protocolHeaders) : of();
  }

  public Map<String, String> getSoapHeaders() {
    return soapHeaders;
  }

  public Map<String, String> getProtocolHeaders() {
    return protocolHeaders;
  }
}
