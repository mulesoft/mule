/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.functional.functional.StringAppendTestTransformer;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class HttpTransformersMule1815TestCase extends FunctionalTestCase {

  public static final String OUTBOUND_MESSAGE = "Test message";

  @Rule
  public DynamicPort dynamicPort1 = new DynamicPort("port1");

  @Rule
  public DynamicPort dynamicPort2 = new DynamicPort("port2");

  @Rule
  public DynamicPort dynamicPort3 = new DynamicPort("port3");

  @Rule
  public DynamicPort dynamicPort4 = new DynamicPort("port4");

  @Override
  protected String getConfigFile() {
    return "http-transformers-mule-1815-test-flow.xml";
  }

  private MuleMessage sendTo(String uri) throws MuleException {
    MuleClient client = muleContext.getClient();
    MuleMessage message = client.send(uri, OUTBOUND_MESSAGE, null).getRight();
    assertNotNull(message);
    return message;
  }

  /**
   * With no transformer we expect just the modification from the FTC
   *
   * @throws Exception
   */
  @Test
  public void testBase() throws Exception {
    assertEquals(OUTBOUND_MESSAGE + " Received", getPayloadAsString(sendTo("base")));
  }

  /**
   * Adapted model, which should not apply transformers
   *
   * @throws Exception
   */
  @Test
  public void testAdapted() throws Exception {
    assertEquals(OUTBOUND_MESSAGE + " Received", getPayloadAsString(sendTo("adapted")));
  }

  /**
   * Change in behaviour: transformers are now always applied as part of inbound flow even if component doesn't invoke them. was:
   * Transformers on the adapted model should be ignored
   *
   * @throws Exception
   */
  @Test
  public void testIgnored() throws Exception {
    assertEquals(OUTBOUND_MESSAGE + " transformed" + " transformed 2" + " Received", getPayloadAsString(sendTo("ignored")));
  }

  /**
   * But transformers on the base model should be applied
   *
   * @throws Exception
   */
  @Test
  public void testInbound() throws Exception {
    assertEquals(
                 // this reads backwards - innermost is first in chain
                 StringAppendTestTransformer.append(" transformed 2", StringAppendTestTransformer.appendDefault(OUTBOUND_MESSAGE))
                     + " Received",
                 getPayloadAsString(sendTo("inbound")));
  }
}
