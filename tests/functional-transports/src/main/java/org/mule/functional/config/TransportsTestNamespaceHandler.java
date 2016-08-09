/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.config;

import org.mule.compatibility.core.endpoint.URIBuilder;
import org.mule.runtime.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.tck.testmodels.mule.TestConnector;

public class TransportsTestNamespaceHandler extends AbstractMuleNamespaceHandler {

  @Override
  public void init() {
    registerStandardTransportEndpoints(TestConnector.TEST, URIBuilder.PATH_ATTRIBUTES);
    registerConnectorDefinitionParser(TestConnector.class);
  }
}
