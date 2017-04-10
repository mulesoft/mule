/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.scripting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.client.MuleClient;

import org.junit.Ignore;
import org.junit.Test;

public class GroovyScriptServiceFunctionalTestCase extends MuleArtifactFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "groovy-component-config.xml";
  }

  @Test
  public void testInlineScript() throws Exception {
    MuleClient client = muleContext.getClient();
    flowRunner("inlineScript").withPayload("Important Message").run();
    Message response = client.request("test://inlineScriptTestOut", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(response);
    assertEquals("Important Message Received", getPayloadAsString(response));
  }

  @Ignore("MULE-6926: flaky test")
  @Test
  public void testFileBasedScript() throws Exception {
    MuleClient client = muleContext.getClient();
    flowRunner("fileBasedScript").withPayload("Important Message").run();
    Message response = client.request("test://fileBasedScriptTestOut", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(response);
    assertEquals("Important Message Received", getPayloadAsString(response));
  }

  @Test
  public void testReferencedScript() throws Exception {
    MuleClient client = muleContext.getClient();
    flowRunner("referencedScript").withPayload("Important Message").run();
    Message response = client.request("test://referencedScriptTestOut", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(response);
    assertEquals("Important Message Received", getPayloadAsString(response));
  }

  @Ignore("MULE-6926: flaky test")
  @Test
  public void testScriptVariables() throws Exception {
    MuleClient client = muleContext.getClient();
    flowRunner("scriptVariables").withPayload("Important Message").run();
    Message response = client.request("test://scriptVariablesTestOut", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(response);
    assertEquals("Important Message Received A-OK", getPayloadAsString(response));
  }
}
