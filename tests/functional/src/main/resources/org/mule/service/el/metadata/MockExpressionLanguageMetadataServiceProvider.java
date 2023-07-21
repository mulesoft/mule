/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.service.el.metadata;

import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.api.service.ServiceDefinition;
import org.mule.runtime.api.service.ServiceProvider;

public class MockExpressionLanguageMetadataServiceProvider implements ServiceProvider {

  private final ExpressionLanguageMetadataService service = new MockExpressionLanguageMetadataService();

  @Override
  public ServiceDefinition getServiceDefinition() {
    return new ServiceDefinition(ExpressionLanguageMetadataService.class, service);
  }
}
