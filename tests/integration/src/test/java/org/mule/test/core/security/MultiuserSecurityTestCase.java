/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_USER_PROPERTY;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.security.DefaultMuleCredentials;
import org.mule.runtime.core.api.security.EncryptionStrategy;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests multi-user security against a security provider which only authenticates a single user at a time (i.e., authentication of
 * a new user overwrites the previous authentication).
 *
 * see EE-979
 */
@Ignore
public class MultiuserSecurityTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"multiuser-security-test-flow.xml", "singleuser-security-provider.xml"};
  }

  @Test
  public void testMultipleAuthentications() throws Exception {
    Message reply;

    reply = getResponse("Data1", "marie");
    assertNotNull(reply);
    assertEquals("user = marie, logins = 1, color = bright red", reply.getPayload().getValue());

    reply = getResponse("Data2", "stan");
    assertNotNull(reply);
    assertEquals("user = stan, logins = 1, color = metallic blue", reply.getPayload().getValue());

    reply = getResponse("Data3", "cindy");
    assertEquals("user = cindy, logins = 1, color = dark violet", reply.getPayload().getValue());

    reply = getResponse("Data4", "marie");
    assertNotNull(reply);
    assertEquals("user = marie, logins = 2, color = bright red", reply.getPayload().getValue());

    reply = getResponse("Data4", "marie");
    assertNotNull(reply);
    assertEquals("user = marie, logins = 3, color = bright red", reply.getPayload().getValue());

    reply = getResponse("Data2", "stan");
    assertNotNull(reply);
    assertEquals("user = stan, logins = 2, color = metallic blue", reply.getPayload().getValue());
  }

  public Message getResponse(String data, String user) throws Exception {
    EncryptionStrategy strategy = muleContext.getSecurityManager().getEncryptionStrategy("PBE");

    Map<String, Serializable> props = new HashMap<>();
    props.put(MULE_USER_PROPERTY, DefaultMuleCredentials.createHeader(user, user, "PBE", strategy));
    return flowRunner("testService").withPayload(data).withInboundProperties(props).run().getMessage();
  }
}
