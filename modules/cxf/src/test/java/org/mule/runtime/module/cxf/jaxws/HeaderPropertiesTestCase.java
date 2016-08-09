/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf.jaxws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.functional.functional.EventCallback;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.apache.hello_world_soap_http.GreeterImpl;
import org.junit.Rule;
import org.junit.Test;

public class HeaderPropertiesTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "header-conf-httpn.xml";
  }

  private GreeterImpl getGreeter() throws Exception {
    Object instance = getComponent("greeterService");

    return (GreeterImpl) instance;
  }

  @Test
  public void testClientWithMuleClient() throws Exception {
    FunctionalTestComponent testComponent = getFunctionalTestComponent("testService");
    assertNotNull(testComponent);

    EventCallback callback = new EventCallback() {

      @Override
      public void eventReceived(final MuleEventContext context, final Object component) throws Exception {
        MuleMessage msg = context.getMessage();
        assertEquals("BAR", msg.getInboundProperty("FOO"));
      }
    };
    testComponent.setEventCallback(callback);

    MuleMessage result = flowRunner("clientFlow").withPayload("Dan").withOutboundProperty("operation", "greetMe")
        .withOutboundProperty("FOO", "BAR").run().getMessage();

    assertEquals("Hello Dan Received", result.getPayload());

    GreeterImpl impl = getGreeter();
    assertEquals(1, impl.getInvocationCount());
  }
}
