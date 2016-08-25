/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_IGNORE_METHOD_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_METHOD_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_USER_PROPERTY;
import static org.mule.runtime.module.http.api.HttpConstants.Methods.POST;
import static org.mule.runtime.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.functional.functional.FunctionalTestNotification;
import org.mule.functional.functional.FunctionalTestNotificationListener;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.runtime.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

public class CxfCustomHttpHeaderTestCase extends FunctionalTestCase implements FunctionalTestNotificationListener {

  private static final HttpRequestOptions HTTP_REQUEST_OPTIONS =
      newOptions().method(POST.name()).disableStatusCodeValidation().build();

  private static final String REQUEST_PAYLOAD = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
      + "<soap:Body>\n" + "<ns1:onReceive xmlns:ns1=\"http://functional.functional.mule.org/\">\n"
      + "    <ns1:arg0 xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xsd:string\">Test String</ns1:arg0>\n"
      + "</ns1:onReceive>\n" + "</soap:Body>\n" + "</soap:Envelope>";

  private static final String SOAP_RESPONSE =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><ns1:onReceiveResponse xmlns:ns1=\"http://functional.functional.mule.org/\"><ns1:return xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"xsd:string\">Test String Received</ns1:return></ns1:onReceiveResponse></soap:Body></soap:Envelope>";

  private List<MuleMessage> notificationMsgList = new ArrayList<>();
  private Latch latch = new Latch();

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "headers-conf-flow-httpn.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    muleContext.registerListener(this);
  }

  @Override
  protected void doTearDown() throws Exception {
    muleContext.unregisterListener(this);
  }

  @Test
  public void testCxf() throws Exception {
    String endpointAddress = "http://localhost:" + dynamicPort.getValue() + "/services/TestComponent";

    String myProperty = "myProperty";

    Map<String, Serializable> props = new HashMap<>();
    props.put(MULE_USER_PROPERTY, "alan");
    props.put(MULE_METHOD_PROPERTY, "onReceive");
    props.put(myProperty, myProperty);

    MuleMessage reply = muleContext.getClient()
        .send(String.format(endpointAddress), MuleMessage.builder().payload(REQUEST_PAYLOAD).outboundProperties(props).build(),
              HTTP_REQUEST_OPTIONS)
        .getRight();

    assertNotNull(reply);
    assertNotNull(reply.getPayload());
    assertEquals(SOAP_RESPONSE, getPayloadAsString(reply));

    latch.await(3000, SECONDS);

    assertEquals(1, notificationMsgList.size());

    // MULE_USER should be allowed in
    assertEquals("alan", notificationMsgList.get(0).getInboundProperty(MULE_USER_PROPERTY));

    // mule properties should be removed
    assertNull(notificationMsgList.get(0).getInboundProperty(MULE_IGNORE_METHOD_PROPERTY));

    // custom properties should be allowed in
    assertEquals(myProperty, notificationMsgList.get(0).getInboundProperty(myProperty));
  }

  @Override
  public void onNotification(ServerNotification notification) {
    if (notification instanceof FunctionalTestNotification) {
      notificationMsgList.add(((FunctionalTestNotification) notification).getEventContext().getMessage());
      latch.release();
    } else {
      fail("invalid notification: " + notification);
    }
  }
}
