/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers;

import static java.lang.String.format;

import org.junit.Test;

public class EndpointServiceConflictTestCase extends AbstractBadConfigTestCase {

  private static final String REPEATED_GLOBAL_NAME = "LenderService";

  @Override
  protected String getConfigFile() {
    return "org/mule/config/spring/parsers/endpoint-service-conflict-test-flow.xml";
  }

  @Test
  public void testBeanError() throws Exception {
    assertErrorContains(format("Two configuration elements have been defined with the same global name. Global name [%s] must be unique",
                               REPEATED_GLOBAL_NAME));
  }

}
