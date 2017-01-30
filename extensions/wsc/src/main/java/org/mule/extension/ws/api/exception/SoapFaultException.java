/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api.exception;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.exception.ErrorMessageAwareException;

import org.apache.cxf.interceptor.Fault;

/**
 * Exception thrown by the Web Service Consumer when processing a SOAP fault. The exception contains the details about the fault.
 *
 * @since 4.0
 */
public class SoapFaultException extends MuleRuntimeException implements ErrorMessageAwareException {

  private final Message message;

  public SoapFaultException(org.apache.cxf.binding.soap.SoapFault cause) {
    super(createStaticMessage(cause.getMessage()));
    SoapFault soapFault = new SoapFault(cause.getFaultCode(),
                                        cause.getSubCode(),
                                        cause.getOrCreateDetail(),
                                        cause.getReason(),
                                        cause.getNode(),
                                        cause.getRole());

    this.message = Message.builder().payload(soapFault).build();
  }

  public SoapFaultException(Fault cause) {
    super(createStaticMessage(cause.getMessage()));
    SoapFault soapFault = new SoapFault(cause.getFaultCode(),
                                        cause.getOrCreateDetail());

    this.message = Message.builder().payload(soapFault).build();
  }

  @Override
  public Message getErrorMessage() {
    return message;
  }

  @Override
  public Throwable getRootCause() {
    return this;
  }
}
