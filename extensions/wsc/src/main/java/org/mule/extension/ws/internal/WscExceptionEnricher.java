/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal;

import static org.mule.extension.ws.api.exception.WscErrors.BAD_REQUEST;
import static org.mule.extension.ws.api.exception.WscErrors.BAD_RESPONSE;
import static org.mule.extension.ws.api.exception.WscErrors.ENCODING;
import static org.mule.extension.ws.api.exception.WscErrors.INVALID_WSDL;
import static org.mule.extension.ws.api.exception.WscErrors.SOAP_FAULT;

import org.mule.extension.ws.api.exception.WscEncodingException;
import org.mule.extension.ws.api.exception.WscException;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.exception.ExceptionHandler;
import org.mule.services.soap.api.exception.BadRequestException;
import org.mule.services.soap.api.exception.BadResponseException;
import org.mule.services.soap.api.exception.InvalidWsdlException;
import org.mule.services.soap.api.exception.SoapFaultException;

/**
 * {@link ExceptionHandler} implementation to wrap unexpected exceptions thrown by the {@link ConsumeOperation} and if a
 * Soap Fault is returned by the server we wrap that exception in a custom WSC {@link SoapFaultException}.
 *
 * @since 4.0
 */
public class WscExceptionEnricher extends ExceptionHandler {

  /**
   * {@inheritDoc}
   */
  @Override
  public Exception enrichException(Exception e) {
    if (e instanceof WscEncodingException) {
      return new ModuleException(e, ENCODING);
    }
    if (e instanceof SoapFaultException) {
      return new ModuleException(e, SOAP_FAULT);
    }
    if (e instanceof InvalidWsdlException) {
      return new ModuleException(e, INVALID_WSDL);
    }
    if (e instanceof BadResponseException) {
      return new ModuleException(e, BAD_RESPONSE);
    }
    if (e instanceof BadRequestException) {
      return new ModuleException(e, BAD_REQUEST);
    }
    if (e instanceof WscException) {
      return e;
    }
    return new WscException("Unexpected error while consuming web service: " + e.getMessage(), e);
  }
}
