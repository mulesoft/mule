/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.spring.remoting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;
import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;

public class SpringRemotingTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort port = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "spring-remoting-mule-config-flow.xml";
  }

  @Test
  public void testHttpInvokeSpringService() throws Exception {
    ComplexData cd = new ComplexData("Foo", new Integer(13));
    HttpInvokerProxyFactoryBean invoker = new HttpInvokerProxyFactoryBean();
    invoker.setServiceInterface(WorkInterface.class);
    invoker.setServiceUrl(String.format("http://localhost:%s/springService", port.getNumber()));
    invoker.afterPropertiesSet();
    WorkInterface worker = (WorkInterface) invoker.getObject();
    ComplexData data = worker.executeComplexity(cd);
    assertNotNull(data);
    assertEquals(data.getSomeString(), "Foo Received");
    assertEquals(data.getSomeInteger(), new Integer(14));
  }
}
