/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.soap.api.message;

import static com.google.common.collect.ImmutableMap.copyOf;
import static com.google.common.collect.ImmutableMap.of;

import java.io.Serializable;
import java.util.Map;

/**
 * Contains the headers retrieved by the protocol after the request.
 *
 * @since 4.0
 */
public final class SoapAttributes implements Serializable {

  private final Map<String, String> protocolHeaders;

  public SoapAttributes(Map<String, String> protocolHeaders) {
    this.protocolHeaders = protocolHeaders != null ? copyOf(protocolHeaders) : of();
  }

  public Map<String, String> getProtocolHeaders() {
    return protocolHeaders;
  }
}
