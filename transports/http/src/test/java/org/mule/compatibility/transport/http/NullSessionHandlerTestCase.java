/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.compatibility.transport.http;

import static junit.framework.Assert.assertNotNull;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore("Session properties are not supported anymore")
public class NullSessionHandlerTestCase extends FunctionalTestCase {

  @Rule
  public DynamicPort httpPort = new DynamicPort("httpPort");

  @Override
  protected String getConfigFile() {
    return "null-session-handler-config.xml";
  }

  @Test
  public void verifiesNoMuleSessionHeader() throws Exception {
    MuleClient client = muleContext.getClient();

    client.dispatch("vm://testInput", TEST_MESSAGE, null);
    MuleMessage response = client.request("vm://testOut", RECEIVE_TIMEOUT).getRight().get();

    assertNotNull("Message was filtered", response);
  }
}
