/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.service.http;

import static java.util.Collections.singletonList;

import org.mule.runtime.api.service.ServiceDefinition;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.http.api.HttpService;

import java.util.List;

public class MockHttpServiceProvider implements ServiceProvider {

  private final HttpService service = new MockHttpService();

  @Override
  public ServiceDefinition getServiceDefinition() {
    return new ServiceDefinition(HttpService.class, service);
  }

}
