/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.plugin.scripting;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class GroovyScriptServiceFunctionalTestCase extends AbstractScriptingFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "groovy-component-config.xml";
  }

  @Test
  public void testInlineScript() throws Exception {
    MuleClient client = AbstractMuleContextTestCase.muleContext.getClient();
    flowRunner("inlineScript").withPayload("Important Message").run();
    Message response = client.request("test://inlineScriptTestOut", AbstractMuleContextTestCase.RECEIVE_TIMEOUT).getRight().get();
    Assert.assertNotNull(response);
    Assert.assertEquals("Important Message Received", getPayloadAsString(response));
  }

  @Ignore("MULE-6926: flaky test")
  @Test
  public void testFileBasedScript() throws Exception {
    MuleClient client = AbstractMuleContextTestCase.muleContext.getClient();
    flowRunner("fileBasedScript").withPayload("Important Message").run();
    Message response =
        client.request("test://fileBasedScriptTestOut", AbstractMuleContextTestCase.RECEIVE_TIMEOUT).getRight().get();
    Assert.assertNotNull(response);
    Assert.assertEquals("Important Message Received", getPayloadAsString(response));
  }

  @Test
  public void testReferencedScript() throws Exception {
    MuleClient client = AbstractMuleContextTestCase.muleContext.getClient();
    flowRunner("referencedScript").withPayload("Important Message").run();
    Message response =
        client.request("test://referencedScriptTestOut", AbstractMuleContextTestCase.RECEIVE_TIMEOUT).getRight().get();
    Assert.assertNotNull(response);
    Assert.assertEquals("Important Message Received", getPayloadAsString(response));
  }

  @Ignore("MULE-6926: flaky test")
  @Test
  public void testScriptVariables() throws Exception {
    MuleClient client = AbstractMuleContextTestCase.muleContext.getClient();
    flowRunner("scriptVariables").withPayload("Important Message").run();
    Message response =
        client.request("test://scriptVariablesTestOut", AbstractMuleContextTestCase.RECEIVE_TIMEOUT).getRight().get();
    Assert.assertNotNull(response);
    Assert.assertEquals("Important Message Received A-OK", getPayloadAsString(response));
  }
}
