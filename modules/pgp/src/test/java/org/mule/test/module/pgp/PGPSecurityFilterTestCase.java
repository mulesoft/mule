/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.pgp;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_USER_PROPERTY;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.message.InternalMessage;
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

    flowRunner("echo")
        .withPayload(eventBuilder().message(InternalMessage.builder().payload(new String(msg)).inboundProperties(props).build())
            .build())
        .run();

    InternalMessage message = client.request("test://output", RECEIVE_TIMEOUT).getRight().get();
    assertEquals("This is a test message.\r\nThis is another line.\r\n", getPayloadAsString(message));
  }

  @Test
  public void testAuthenticationNotAuthorised() throws Exception {
    Event replyEvent =
        flowRunner("echo").withPayload("An unsigned message").withInboundProperties(createMessageProperties()).run();
    assertThat(replyEvent.getError().isPresent(), is(true));
    assertThat(replyEvent.getError().get().getDetailedDescription(), is(MESSAGE_EXCEPTION));
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
