/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.metadata;

import javax.wsdl.extensions.soap.SOAPHeader;
import javax.wsdl.extensions.soap12.SOAP12Header;
import javax.xml.namespace.QName;

/**
 * Represents a Soap Header, it does not take in mind if this is a SOAP 1.1 or SOAP 1.2 header.
 *
 * @since 4.0
 */
public class SoapHeaderAdapter {

  private final QName message;
  private final String part;
  private final String namespace;

  public SoapHeaderAdapter(QName message, String part, String namespace) {
    this.message = message;
    this.part = part;
    this.namespace = namespace;
  }

  public SoapHeaderAdapter(SOAPHeader header) {
    this.message = header.getMessage();
    this.part = header.getPart();
    this.namespace = header.getNamespaceURI();
  }

  public SoapHeaderAdapter(SOAP12Header header) {
    this.message = header.getMessage();
    this.part = header.getPart();
    this.namespace = header.getNamespaceURI();
  }

  /**
   * Get the message for this header.
   */
  public QName getMessage() {
    return message;
  }

  /**
   * Get the part for this header.
   */
  public String getPart() {
    return part;
  }

  /**
   * Get the namespace URI for this header.
   */
  public String getNamespaceURI() {
    return namespace;
  }

}
