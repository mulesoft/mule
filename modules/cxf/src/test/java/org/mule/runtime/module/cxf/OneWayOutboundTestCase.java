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
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.ACCEPTED;
import static org.mule.runtime.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.module.cxf.testmodels.AsyncService;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class OneWayOutboundTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort httpPort = new DynamicPort("httpPort");

  @Override
  protected String getConfigFile() {
    return "one-way-outbound-config.xml";
  }

  @Test
  public void jaxwsClientSupportsOneWayCall() throws Exception {
    MuleEvent event = flowRunner("jaxwsClient").withPayload(TEST_MESSAGE).run();
    assertOneWayResponse(event);
  }

  @Test
  public void proxyClientSupportsOneWayCall() throws Exception {
    String message = "<ns:send xmlns:ns=\"http://testmodels.cxf.module.runtime.mule.org/\"><text>hello</text></ns:send>";
    MuleEvent event = flowRunner("proxyClient").withPayload(message).run();
    assertOneWayResponse(event);
  }

  private void assertOneWayResponse(MuleEvent event) throws Exception {
    assertThat(event.getMessage().getPayload(), is(nullValue()));
    assertThat(event.getMessage().<Integer>getInboundProperty(HTTP_STATUS_PROPERTY), is(ACCEPTED.getStatusCode()));

    AsyncService component = (AsyncService) getComponent("asyncService");
    assertTrue(component.getLatch().await(RECEIVE_TIMEOUT, MILLISECONDS));
  }
}
