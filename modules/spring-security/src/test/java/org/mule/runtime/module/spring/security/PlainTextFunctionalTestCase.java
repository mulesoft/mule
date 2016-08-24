/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.spring.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_USER_PROPERTY;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.UNAUTHORIZED;
import static org.mule.runtime.module.http.api.HttpConstants.RequestProperties.HTTP_STATUS_PROPERTY;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.security.MuleCredentials;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore("See MULE-9202")
public class PlainTextFunctionalTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort port1 = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    // Note that this file contains global attributes, which the configuration-building
    // process will ignore (MULE-5375)
    return "encryption-test-flow.xml";
  }

  @Test
  public void testAuthenticationFailureNoContext() throws Exception {
    org.mule.runtime.core.api.client.MuleClient client = muleContext.getClient();
    MuleMessage m = client.send(getUrl(), getTestMuleMessage()).getRight();
    assertNotNull(m);
    int status = m.getInboundProperty(HTTP_STATUS_PROPERTY, -1);
    assertEquals(UNAUTHORIZED.getStatusCode(), status);
  }

  @Test
  public void testAuthenticationFailureBadCredentials() throws Exception {
    MuleClient client = muleContext.getClient();

    MuleMessage message = createRequestMessage("anonX", "anonX");
    MuleMessage response = client.send(getUrl(), message).getRight();
    assertNotNull(response);
    int status = response.getInboundProperty(HTTP_STATUS_PROPERTY, -1);
    assertEquals(UNAUTHORIZED.getStatusCode(), status);
  }

  @Test
  public void testAuthenticationAuthorised() throws Exception {
    MuleClient client = muleContext.getClient();

    MuleMessage message = createRequestMessage("anon", "anon");
    MuleMessage response = client.send(getUrl(), message).getRight();
    assertNotNull(response);
    int status = response.getInboundProperty(HTTP_STATUS_PROPERTY, -1);
    assertEquals(OK, status);
  }

  private MuleMessage createRequestMessage(String user, String password) {
    String header = MuleCredentials.createHeader(user, password.toCharArray());
    return MuleMessage.builder().payload(TEST_PAYLOAD).addOutboundProperty(MULE_USER_PROPERTY, header).build();
  }

  private String getUrl() {
    return String.format("http://localhost:%s/index.html", port1.getNumber());
  }
}
