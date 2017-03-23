/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap;

import org.mule.service.http.api.HttpService;
import org.mule.services.soap.api.SoapService;
import org.mule.services.soap.api.client.SoapClientFactory;
import org.mule.services.soap.client.SoapCxfClientFactory;

/**
 * Default Mule {@link SoapService} implementation.
 *
 * @since 4.0
 */
public class SoapServiceImplementation implements SoapService {

  private HttpService httpService;

  public SoapServiceImplementation(HttpService httpService) {
    this.httpService = httpService;
  }

  @Override
  public String getName() {
    return "SOAP Service";
  }

  @Override
  public SoapClientFactory getClientFactory() {
    return new SoapCxfClientFactory(httpService);
  }
}
