/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf.wssec;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;

import org.mule.compatibility.module.cxf.AbstractCxfOverHttpExtensionTestCase;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.tck.junit4.rule.DynamicPort;

import javax.xml.ws.soap.SOAPFaultException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class WsCustomValidatorTestCase extends AbstractCxfOverHttpExtensionTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String getConfigFile() {
    return "org/mule/compatibility/module/cxf/wssec/ws-custom-validator-config-httpn.xml";
  }

  @Test
  public void testSuccessfulAuthentication() throws Exception {
    ClientPasswordCallback.setPassword("secret");
    InternalMessage received = flowRunner("cxfClient").withPayload("me").run().getMessage();

    assertNotNull(received);
    assertEquals("Hello me", getPayloadAsString(received));

  }

  @Test
  public void testFailAuthentication() throws Exception {
    ClientPasswordCallback.setPassword("wrongPassword");
    expectedException.expectCause(hasCause(instanceOf(SOAPFaultException.class)));
    expectedException.expectMessage("The security token could not be authenticated");
    flowRunner("cxfClient").withPayload("hello").run();
  }
}
