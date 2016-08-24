/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.spring.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;

import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.context.notification.SecurityNotificationListener;
import org.mule.runtime.core.context.notification.SecurityNotification;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.runtime.core.util.concurrent.Latch;

import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;

public class SecureHttpPollingFunctionalTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort port1 = new DynamicPort("port1");


  @Override
  protected String[] getConfigFiles() {
    return new String[] {"secure-http-polling-server-flow.xml", "secure-http-polling-client-flow.xml"};
  }

  @Test
  public void testPollingHttpConnectorSentCredentials() throws Exception {
    final Latch latch = new Latch();
    muleContext.registerListener(new SecurityNotificationListener<SecurityNotification>() {

      @Override
      public void onNotification(SecurityNotification notification) {
        latch.countDown();
      }
    });

    MuleClient client = muleContext.getClient();
    MuleMessage result = client.request("test://toclient", 5000).getRight().get();
    assertNotNull(result);
    assertEquals("foo", getPayloadAsString(result));

    result = client.request("test://toclient2", 1000).getRight().get();
    // This seems a little odd that we forward the exception to the outbound endpoint, but I guess users
    // can just add a filter
    assertNotNull(result);
    int status = result.getInboundProperty(HTTP_STATUS_PROPERTY, 0);
    assertEquals(401, status);
    assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
  }
}
