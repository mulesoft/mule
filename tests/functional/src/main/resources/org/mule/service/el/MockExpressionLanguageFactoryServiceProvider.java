/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
  public List<ServiceDefinition> providedServices() {
    return singletonList(new ServiceDefinition(DefaultExpressionLanguageFactoryService.class, service));
  }

}
