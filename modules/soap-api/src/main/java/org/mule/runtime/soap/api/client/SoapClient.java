/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.soap.api.client;

import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.soap.api.client.metadata.SoapMetadataResolver;
import org.mule.runtime.soap.api.message.SoapRequest;
import org.mule.runtime.soap.api.message.SoapResponse;

/**
 * Contract for clients that consumes SOAP Web Services, and returns the response.
 *
 * @since 4.0
 */
public interface SoapClient extends Startable, Stoppable {

  /**
   * Sends a {@link SoapRequest} blocking the current thread until a response is available or the request times out.
   *
   * @param request a {@link SoapRequest} instance.
   * @return a {@link SoapResponse} instance with the XML content and Headers if any.
   */
  SoapResponse consume(SoapRequest request);

  /**
   * @return a {@link SoapMetadataResolver} that can resolve the INPUT and OUTPUT metadata for the different Web Service Operations.
   */
  SoapMetadataResolver getMetadataResolver();
}
