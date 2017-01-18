/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf.jaxws;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.mule.compatibility.module.cxf.AbstractCxfOverHttpExtensionTestCase;
import org.mule.extension.http.api.HttpAttributes;
import org.mule.functional.functional.EventCallback;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.runtime.api.message.Message;
import org.mule.tck.junit4.rule.DynamicPort;

import org.apache.hello_world_soap_http.GreeterImpl;
import org.junit.Rule;
import org.junit.Test;

public class HeaderPropertiesTestCase extends AbstractCxfOverHttpExtensionTestCase {

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

    EventCallback callback = (context, component, muleContext) -> {
      Message msg = context.getMessage();
      // TODO MULE-9857 Make message properties case sensitive
      assertThat(((HttpAttributes) msg.getAttributes()).getHeaders().get("FOO".toLowerCase()), is("BAR"));
    };
    testComponent.setEventCallback(callback);

    Message result = flowRunner("clientFlow").withPayload("Dan").withOutboundProperty("operation", "greetMe")
        .withOutboundProperty("FOO", "BAR").run().getMessage();

    assertEquals("Hello Dan Received", result.getPayload().getValue());

    GreeterImpl impl = getGreeter();
    assertEquals(1, impl.getInvocationCount());
  }
}
