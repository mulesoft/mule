/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api.exception;

import static java.util.Optional.ofNullable;

import java.util.Optional;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

/**
 * A simple POJO that represents a SOAP Fault that occurs during invocation processing.
 *
 * @since 4.0
 */
public class SoapFault {

  private final QName faultCode;
  private final QName subCode;
  private final Element detail;
  private final String reason;
  private final String node;
  private final String role;

  public SoapFault(QName faultCode, QName subCode, Element detail, String reason, String node, String role) {
    this.faultCode = faultCode;
    this.subCode = subCode;
    this.detail = detail;
    this.reason = reason;
    this.node = node;
    this.role = role;
  }

  public SoapFault(QName faultCode, Element detail) {
    this.faultCode = faultCode;
    this.detail = detail;
    reason = "";
    node = null;
    role = null;
    subCode = null;
  }

  /**
   * @return a code used to indicate a class of errors.
   */
  public QName getFaultCode() {
    return faultCode;
  }

  /**
   * @return the cause (reason) of the fault.
   */
  public String getReason() {
    return reason;
  }

  /**
   * @return an Optional Subcode element if you need to break down the {@link this#getFaultCode()} into subcodes.
   */
  public Optional<QName> getSubCode() {
    return ofNullable(subCode);
  }

  /**
   * @return an element that carries application-specific error messages. It can contain child elements called detail entries.
   */
  public Element getDetail() {
    return detail;
  }

  /**
   * @return an URI identifying the node in which the Fault occurred.
   */
  public Optional<String> getNode() {
    return ofNullable(node);
  }

  /**
   * @return the role of the node in which the fault occurred.
   */
  public Optional<String> getRole() {
    return ofNullable(role);
  }
}
