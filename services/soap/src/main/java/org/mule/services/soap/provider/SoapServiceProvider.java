/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.provider;

import static java.util.Collections.singletonList;
import org.mule.runtime.api.service.ServiceDefinition;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.service.http.api.HttpService;
import org.mule.services.soap.api.SoapService;
import org.mule.services.soap.SoapServiceImplementation;

import java.util.List;

import javax.inject.Inject;

/**
 * {@link ServiceProvider} implementation for providing a mule {@link SoapService}.
 *
 * @since 4.0
 */
public class SoapServiceProvider implements ServiceProvider {

  @Inject
  private HttpService httpService;

  @Override
  public List<ServiceDefinition> providedServices() {
    ServiceDefinition serviceDefinition = new ServiceDefinition(SoapService.class, new SoapServiceImplementation(httpService));
    return singletonList(serviceDefinition);
  }
}
