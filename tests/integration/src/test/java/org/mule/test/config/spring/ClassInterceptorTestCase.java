/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring;

import org.junit.Test;

public class ClassInterceptorTestCase extends AbstractInterceptorTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/config/spring/class-interceptor-test-flow.xml";
  }

  @Test
  public void testInterceptor() throws Exception {
    flowRunner("service").withPayload(MESSAGE).asynchronously().run();

    assertMessageIntercepted();
  }
}
