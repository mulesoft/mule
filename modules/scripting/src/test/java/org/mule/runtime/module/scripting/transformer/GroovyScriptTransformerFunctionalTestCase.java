/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.scripting.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.functional.junit4.FunctionalTestCase;

import org.junit.Test;

public class GroovyScriptTransformerFunctionalTestCase extends FunctionalTestCase {

  public GroovyScriptTransformerFunctionalTestCase() {
    // Groovy really hammers the startup time since it needs to create the interpreter on every start
    setDisposeContextPerClass(false);
  }

  @Override
  protected String getConfigFile() {
    return "groovy-transformer-config-flow.xml";
  }

  @Test
  public void testInlineScript() throws Exception {
    MuleClient client = muleContext.getClient();
    flowRunner("inlineScript").withPayload("hello").asynchronously().run();
    MuleMessage response = client.request("test://inlineScriptTestOut", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(response);
    assertEquals("hexxo", response.getPayload());
  }

  @Test
  public void testFileBasedScript() throws Exception {
    MuleClient client = muleContext.getClient();
    flowRunner("fileBasedScript").withPayload("hello").asynchronously().run();
    MuleMessage response = client.request("test://fileBasedScriptTestOut", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(response);
    assertEquals("hexxo", response.getPayload());
  }

  @Test
  public void testReferencedTransformer() throws Exception {
    MuleClient client = muleContext.getClient();
    flowRunner("referencedTransformer").withPayload("hello").asynchronously().run();
    MuleMessage response = client.request("test://referencedTransformerTestOut", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(response);
    assertEquals("hexxo", response.getPayload());
  }

  @Test
  public void testReferencedTransformerWithParameters() throws Exception {
    MuleClient client = muleContext.getClient();
    flowRunner("referencedTransformerWithParameters").withPayload("hello").asynchronously().run();
    MuleMessage response = client.request("test://referencedTransformerWithParametersTestOut", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(response);
    assertEquals("hexxo", response.getPayload());
  }

  @Test
  public void transformByAssigningPayload() throws Exception {
    MuleMessage response = flowRunner("transformByAssigningPayload").withPayload("hello").run().getMessage();
    assertNotNull(response);
    assertEquals("bar", response.getPayload());
  }

  @Test
  public void transformByAssigningHeader() throws Exception {
    MuleMessage response = flowRunner("transformByAssigningProperty").withPayload("hello").run().getMessage();
    assertNotNull(response);
    assertEquals("hello", response.getPayload());
    assertEquals("bar", response.getOutboundProperty("foo"));
  }


}
