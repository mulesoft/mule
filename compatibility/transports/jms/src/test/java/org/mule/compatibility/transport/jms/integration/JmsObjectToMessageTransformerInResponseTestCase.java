/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms.integration;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.client.MuleClient;

import org.junit.Test;

public class JmsObjectToMessageTransformerInResponseTestCase extends AbstractJmsFunctionalTestCase {

  public static final int TIMEOUT = 3000;

  @Override
  protected String getConfigFile() {
    return "integration/jms-object-to-message-transformer-test-case.xml";
  }

  @Test
  public void testObjectToMessageDoesntFail() throws Exception {
    MuleClient muleClient = muleContext.getClient();
    InternalMessage response = muleClient.send("inWithTransformers", "A message", null, TIMEOUT).getRight();
    assertThat(response, notNullValue());
    assertThat(getPayloadAsString(response), is("A message with something more"));
  }
}
