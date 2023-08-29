/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
