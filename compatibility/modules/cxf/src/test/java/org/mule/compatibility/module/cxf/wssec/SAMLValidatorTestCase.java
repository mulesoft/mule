/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf.wssec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.compatibility.module.cxf.AbstractCxfOverHttpExtensionTestCase;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.ws.security.WSPasswordCallback;
import org.junit.Rule;
import org.junit.Test;

public class SAMLValidatorTestCase extends AbstractCxfOverHttpExtensionTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "org/mule/compatibility/module/cxf/wssec/saml-validator-conf-httpn.xml";
  }

  @Test
  public void testSAMLUnsignedAssertion() throws Exception {
    InternalMessage received = flowRunner("cxfClient").withPayload("me").run().getMessage();

    assertNotNull(received);
    assertEquals("Hello me", getPayloadAsString(received));
  }

  @Test
  public void testSAMLSignedAssertion() throws Exception {
    InternalMessage received = flowRunner("cxfClientSigned").withPayload("me").run().getMessage();

    assertNotNull(received);
    assertEquals("Hello me", getPayloadAsString(received));
  }

  public static class PasswordCallbackHandler implements CallbackHandler {

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
      WSPasswordCallback pc = (WSPasswordCallback) callbacks[0];

      pc.setPassword("secret");
    }
  }
}
