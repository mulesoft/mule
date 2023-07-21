/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.service.foo;

import org.mule.runtime.api.service.ServiceDefinition;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.service.test.api.EchoService;
import org.mule.runtime.service.test.api.FooService;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class FooServiceProvider implements ServiceProvider {

  @Inject
  private EchoService echoService;

  @Override
  public ServiceDefinition getServiceDefinition() {
    return new ServiceDefinition(FooService.class, new DefaultFooService(echoService));
  }
}
