/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal;

import org.mule.extension.ws.api.exception.InvalidWsdlException;
import org.mule.extension.ws.api.exception.SoapFaultException;
import org.mule.extension.ws.api.exception.WscEncodingException;
import org.mule.extension.ws.api.exception.WscException;
import org.mule.runtime.extension.api.runtime.exception.ExceptionEnricher;

import org.apache.cxf.binding.soap.SoapFault;

/**
 * {@link ExceptionEnricher} implementation to wrap unexpected exceptions thrown by the {@link ConsumeOperation} and if a
 * Soap Fault is returned by the server we wrap that exception in a custom WSC {@link SoapFaultException}.
 *
 * @since 4.0
 */
public class WscExceptionEnricher implements ExceptionEnricher {

  /**
   * {@inheritDoc}
   * <p>
   * Wraps Soap Faults in a custom {@link SoapFaultException} otherwise return a {@link WscException} wrapping the unexpected
   * exception.
   */
  @Override
  public Exception enrichException(Exception e) {
    if (e instanceof SoapFault) {
      SoapFault sf = (SoapFault) e;
      // TODO build proper error type.
      return new SoapFaultException(sf.getFaultCode(), sf.getSubCode(), sf.getMessage(), sf.getDetail());
    }
    if (e instanceof WscException) {
      // TODO build proper error type.
      return e;
    }
    if (e instanceof WscEncodingException) {
      // TODO build proper error type.
      return e;
    }
    if (e instanceof InvalidWsdlException) {
      // TODO build proper error type.
      return e;
    }
    return new WscException("Unexpected error while consuming web service", e);
  }
}
