/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.http.impl.provider;

import org.mule.runtime.api.service.ServiceDefinition;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.service.http.api.HttpService;
import org.mule.services.http.impl.service.HttpServiceImplementation;

import com.google.common.collect.Lists;

import java.util.List;

public class HttpServiceProvider implements ServiceProvider {

  HttpServiceImplementation service = new HttpServiceImplementation();
  private ServiceDefinition serviceDefinition = new ServiceDefinition(HttpService.class, service);

  @Override
  public List<ServiceDefinition> providedServices() {
    return Lists.newArrayList(serviceDefinition);
  }
}
