/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.echo;

import org.mule.runtime.api.service.ServiceDefinition;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.service.test.api.EchoService;

public class EchoServiceProvider implements ServiceProvider {

  @Override
  public ServiceDefinition getServiceDefinition() {
    return new ServiceDefinition(EchoService.class, new DefaultEchoService());
  }
}
