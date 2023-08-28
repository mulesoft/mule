/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.http;

import org.mule.runtime.api.service.ServiceDefinition;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.http.api.HttpService;

public class ResourceHttpServiceProvider implements ServiceProvider {

  @Override
  public ServiceDefinition getServiceDefinition() {
    return new ServiceDefinition(HttpService.class, new ResourceHttpService());
  }
}
