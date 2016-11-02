/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api.exception;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.exception.MuleRuntimeException;

import java.util.Optional;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

/**
 * Exception thrown by the Web Service Consumer when processing a SOAP fault. The exception contains the details about the fault.
 *
 * @since 4.0
 */
public class SoapFaultException extends MuleRuntimeException {

  private final QName faultCode;
  private final QName subCode;
  private final Element detail;

  public SoapFaultException(QName faultCode,
                            QName subCode,
                            String message,
                            Element detail) {
    super(createStaticMessage(message));
    this.faultCode = faultCode;
    this.subCode = subCode;
    this.detail = detail;
  }

  public SoapFaultException(QName faultCode, String message, Element detail) {
    super(createStaticMessage(message));
    this.faultCode = faultCode;
    this.subCode = null;
    this.detail = detail;
  }

  public QName getFaultCode() {
    return faultCode;
  }

  public Optional<QName> getSubCode() {
    return ofNullable(subCode);
  }

  public Element getDetail() {
    return detail;
  }
}
