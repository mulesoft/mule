/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.api.message;

/**
 * Represents and enables the construction of an header to be used with SOAP.
 *
 * @since 4.0
 */
public final class SoapHeader {

  private final String id;
  private final String value;

  public SoapHeader(String id, String value) {
    this.id = id;
    this.value = value;
  }

  public String getId() {
    return id;
  }

  public String getValue() {
    return value;
  }
}
