/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf;


import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.extension.http.api.HttpConstants.HttpStatus.ACCEPTED;

import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.extension.http.internal.HttpConnector;
import org.mule.extension.socket.api.SocketsExtension;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.module.cxf.testmodels.AsyncService;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class OneWayOutboundTestCase extends ExtensionFunctionalTestCase {

  @Rule
  public DynamicPort httpPort = new DynamicPort("httpPort");

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class[] {SocketsExtension.class, HttpConnector.class};
  }

  @Override
  protected String getConfigFile() {
    return "one-way-outbound-config.xml";
  }

  @Test
  public void jaxwsClientSupportsOneWayCall() throws Exception {
    Event event = flowRunner("jaxwsClient").withPayload(TEST_MESSAGE).run();
    assertOneWayResponse(event);
  }

  @Test
  public void proxyClientSupportsOneWayCall() throws Exception {
    String message = "<ns:send xmlns:ns=\"http://testmodels.cxf.module.runtime.mule.org/\"><text>hello</text></ns:send>";
    Event event = flowRunner("proxyClient").withPayload(message).run();
    assertOneWayResponse(event);
  }

  private void assertOneWayResponse(Event event) throws Exception {
    assertThat(event.getMessage().getPayload().getValue(), is(nullValue()));
    assertThat(((HttpResponseAttributes) event.getMessage().getAttributes()).getStatusCode(), is(ACCEPTED.getStatusCode()));

    AsyncService component = (AsyncService) getComponent("asyncService");
    assertTrue(component.getLatch().await(RECEIVE_TIMEOUT, MILLISECONDS));
  }
}
