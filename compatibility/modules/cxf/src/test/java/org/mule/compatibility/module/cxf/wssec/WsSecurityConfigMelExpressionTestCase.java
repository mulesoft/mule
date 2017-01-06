/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf.wssec;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.mule.compatibility.module.cxf.AbstractCxfOverHttpExtensionTestCase;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.tck.junit4.rule.DynamicPort;

import javax.xml.ws.soap.SOAPFaultException;

import org.junit.Rule;
import org.junit.Test;

public class WsSecurityConfigMelExpressionTestCase extends AbstractCxfOverHttpExtensionTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "org/mule/compatibility/module/cxf/wssec/ws-security-config-mel-expression-config-httpn.xml";
  }

  @Test
  public void testSuccessfulAuthentication() throws Exception {
    ClientPasswordCallback.setPassword("secret");
    InternalMessage received = flowRunner("cxfClient").withPayload("PasswordText").run().getMessage();

    assertNotNull(received);
    assertEquals("Hello PasswordText", getPayloadAsString(received));
  }

  @Test
  public void testFailAuthentication() throws Exception {
    ClientPasswordCallback.setPassword("secret");
    Exception e = flowRunner("cxfClient").withPayload("UnknownPasswordEncoding").runExpectingException();

    assertThat(e, is(instanceOf(MessagingException.class)));
    assertThat(e.getCause().getCause().getCause(), is(instanceOf(SOAPFaultException.class)));
    assertThat(e.getCause().getCause().getMessage(), containsString("Security exception occurred invoking web service"));
  }
}
