/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
