/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.pgp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_USER_PROPERTY;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.ExceptionPayload;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.util.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

@Ignore("MULE-9818")
public class PGPSecurityFilterTestCase extends FunctionalTestCase {

  protected static final String TARGET = "/encrypted.txt";
  protected static final String MESSAGE_EXCEPTION = "Crypto Failure";


  @Override
  protected String getConfigFile() {
    return "test-pgp-encrypt-config-flow.xml";
  }

  @Test
  public void testAuthenticationAuthorised() throws Exception {
    MuleClient client = muleContext.getClient();

    byte[] msg = loadEncryptedMessage();
    Map<String, Serializable> props = createMessageProperties();

    flowRunner("echo").withPayload(getTestEvent(MuleMessage.builder().payload(new String(msg)).inboundProperties(props).build()))
        .asynchronously().run();

    MuleMessage message = client.request("test://output", RECEIVE_TIMEOUT).getRight().get();
    assertEquals("This is a test message.\r\nThis is another line.\r\n", getPayloadAsString(message));
  }

  @Test
  public void testAuthenticationNotAuthorised() throws Exception {
    MuleEvent replyEvent =
        flowRunner("echo").withPayload("An unsigned message").withInboundProperties(createMessageProperties()).run();
    assertNotNull(replyEvent.getError());
    assertEquals(MESSAGE_EXCEPTION, replyEvent.getError().getDetailedDescription());
  }

  private byte[] loadEncryptedMessage() throws IOException {
    URL url = Thread.currentThread().getContextClassLoader().getResource("./encrypted-compressed-signed.asc");

    FileInputStream in = new FileInputStream(url.getFile());
    byte[] msg = IOUtils.toByteArray(in);
    in.close();

    return msg;
  }

  private Map<String, Serializable> createMessageProperties() {
    Map<String, Serializable> props = new HashMap<>();
    props.put("TARGET_FILE", TARGET);
    props.put(MULE_USER_PROPERTY, "Mule server <mule_server@mule.com>");
    return props;
  }
}
