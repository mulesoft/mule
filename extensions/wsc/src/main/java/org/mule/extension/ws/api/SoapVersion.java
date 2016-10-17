/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api;

import static javax.xml.soap.SOAPConstants.SOAP_1_1_PROTOCOL;
import static javax.xml.soap.SOAPConstants.SOAP_1_2_PROTOCOL;

public enum SoapVersion {

  SOAP11("1.1", SOAP_1_1_PROTOCOL), SOAP12("1.2", SOAP_1_2_PROTOCOL);

  private String version;
  private String protocol;

  SoapVersion(String version, String protocol) {
    this.version = version;
    this.protocol = protocol;
  }

  public String getVersion() {
    return version;
  }

  public String getProtocol() {
    return protocol;
  }
}
