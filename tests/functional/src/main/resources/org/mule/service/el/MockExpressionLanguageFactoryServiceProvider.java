/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.service.el;

import static java.util.Collections.singletonList;

import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.api.service.ServiceDefinition;
import org.mule.runtime.api.service.ServiceProvider;

import java.util.List;

public class MockExpressionLanguageFactoryServiceProvider implements ServiceProvider {

  private final DefaultExpressionLanguageFactoryService service = new MockExpressionLanguageFactoryService();

  @Override
  public ServiceDefinition getServiceDefinition() {
    return new ServiceDefinition(DefaultExpressionLanguageFactoryService.class, service);
  }
}
