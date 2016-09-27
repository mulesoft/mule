/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.spring.security;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.extension.http.internal.HttpConnector;
import org.mule.extension.socket.api.SocketsExtension;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.context.notification.SecurityNotificationListener;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.context.notification.SecurityNotification;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class SecureHttpPollingFunctionalTestCase extends ExtensionFunctionalTestCase {

  @Rule
  public DynamicPort port1 = new DynamicPort("port1");

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class[] {SocketsExtension.class, HttpConnector.class};
  }

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"secure-http-polling-server-flow.xml", "secure-http-polling-client-flow.xml"};
  }

  @Test
  public void testPollingHttpConnectorSentCredentials() throws Exception {
    final Latch latch = new Latch();
    muleContext.registerListener((SecurityNotificationListener<SecurityNotification>) notification -> latch.countDown());

    MuleClient client = muleContext.getClient();
    InternalMessage result = client.request("test://toclient", 5000).getRight().get();
    assertThat(result, not(nullValue()));
    assertThat(getPayloadAsString(result), is("foo"));

    result = client.request("test://toclient2", 1000).getRight().get();
    // This seems a little odd that we forward the exception to the outbound endpoint, but I guess users
    // can just add a filter
    assertThat(result, not(nullValue()));
    assertThat(result.getAttributes(), instanceOf(HttpResponseAttributes.class));
    assertThat(((HttpResponseAttributes) result.getAttributes()).getStatusCode(), is(401));
    assertThat(latch.await(1000, MILLISECONDS), is(true));
  }
}
