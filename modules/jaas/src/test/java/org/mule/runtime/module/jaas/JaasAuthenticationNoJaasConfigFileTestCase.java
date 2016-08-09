/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.jaas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.processor.MessageProcessor;

import org.junit.Test;

public class JaasAuthenticationNoJaasConfigFileTestCase extends AbstractJaasFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "mule-conf-with-no-jaas-config-file-flow.xml";
  }

  @Test
  public void goodAuthentication() throws Exception {
    SecurityHeader securityHeader = createSecurityHeader("Marie.Rizzo", "dragon");
    MuleMessage message = flowRunner("TestUMO").withInboundProperty(securityHeader.getKey(), securityHeader.getValue())
        .withPayload("Test").run().getMessage();

    assertNotNull(message);
    assertTrue(message.getPayload() instanceof String);
    assertEquals("Test Received", getPayloadAsString(message));
  }

  @Test
  public void anotherGoodAuthentication() throws Exception {
    SecurityHeader securityHeader = createSecurityHeader("anon", "anon");
    MuleMessage message = flowRunner("TestUMO").withInboundProperty(securityHeader.getKey(), securityHeader.getValue())
        .withPayload("Test").run().getMessage();

    assertNotNull(message);
    assertTrue(message.getPayload() instanceof String);
    assertEquals("Test Received", getPayloadAsString(message));
  }

  @Test
  public void wrongCombinationOfCorrectUsernameAndPassword() throws Exception {
    SecurityHeader securityHeader = createSecurityHeader("Marie.Rizzo", "anon");
    runFlowAndExpectUnauthorizedException(securityHeader);
  }

  @Test
  public void badUserName() throws Exception {
    SecurityHeader securityHeader = createSecurityHeader("Evil", "dragon");
    runFlowAndExpectUnauthorizedException(securityHeader);
  }

  @Test
  public void badPassword() throws Exception {
    SecurityHeader securityHeader = createSecurityHeader("Marie.Rizzo", "evil");
    runFlowAndExpectUnauthorizedException(securityHeader);
  }

  public static class AddNotSerializableProperty implements MessageProcessor {

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException {
      event.setFlowVariable("notSerializableProperty", new Object());
      return event;
    }
  }
}
