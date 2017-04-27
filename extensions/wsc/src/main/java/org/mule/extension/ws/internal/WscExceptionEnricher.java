/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal;

import org.mule.runtime.extension.api.runtime.exception.ExceptionHandler;
import org.mule.services.soap.api.exception.SoapFaultException;
import org.mule.services.soap.internal.exception.error.SoapExceptionEnricher;

/**
 * {@link ExceptionHandler} implementation to wrap unexpected exceptions thrown by the {@link ConsumeOperation} and if a
 * Soap Fault is returned by the server we wrap that exception in a custom WSC {@link SoapFaultException}.
 *
 * @since 4.0
 */
public class WscExceptionEnricher extends ExceptionHandler {

  private final SoapExceptionEnricher enricher = new SoapExceptionEnricher();

  /**
   * {@inheritDoc}
   */
  @Override
  public Exception enrichException(Exception e) {
    return enricher.enrich(e);
  }
}
