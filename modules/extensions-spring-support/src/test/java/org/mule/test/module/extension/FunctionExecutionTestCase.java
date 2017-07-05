/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;

import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class FunctionExecutionTestCase extends AbstractExtensionFunctionalTestCase {

  private static final String FUNCTIONS_CONFIG_XML = "functions-config.xml";
  private ExtendedExpressionManager expressionManager;
  private DocumentBuilderFactory documentBuilderFactory;

  @Override
  protected String[] getConfigFiles() {
    return new String[] {FUNCTIONS_CONFIG_XML};
  }

  @Override
  protected boolean isDisposeContextPerClass() {
    return true;
  }

  @Before
  public void setupManager() {
    expressionManager = muleContext.getExpressionManager();
    documentBuilderFactory = DocumentBuilderFactory.newInstance();
  }

  @Test
  public void echoFromManager() throws Exception {
    TypedValue result = expressionManager.evaluate("customEcho('myMessage')");
    assertThat(result.getValue(), is("myMessage"));
  }

  @Test
  public void echoWithDefault() throws Exception {
    Object value = flowRunner("echoWithDefault").withPayload("sampleData")
        .run().getMessage().getPayload().getValue();
    assertThat(value, is("prefix_sampleData"));
  }

  @Test
  public void payloadGlobalEcho() throws Exception {
    Object value = flowRunner("payloadGlobalEcho").withPayload(10).run().getMessage().getPayload().getValue();
    assertThat(value, is("10"));
  }

  @Test
  public void variableGlobalEcho() throws Exception {
    Object value = flowRunner("variableGlobalEcho").withVariable("myMessage", "sampleData")
        .run().getMessage().getPayload().getValue();
    assertThat(value, is("sampleData"));
  }

  @Test
  public void toMap() throws Exception {
    Map<String, String> value = (Map<String, String>) flowRunner("toMap").run().getMessage().getPayload().getValue();
    assertThat(value.get("user"), is("pepe"));
  }

  @Test
  public void xpathWithDefaults() throws Exception {
    Document document = documentBuilderFactory.newDocumentBuilder()
        .parse(Thread.currentThread().getContextClassLoader().getResourceAsStream(FUNCTIONS_CONFIG_XML));
    Object value = flowRunner("xpathWithDefaults").withPayload(document).run().getMessage().getPayload().getValue();
    assertThat(value, is("xpathWithDefaults"));
  }

  @Test
  public void xpathWithDefaultNode() throws Exception {
    Document document = documentBuilderFactory.newDocumentBuilder()
        .parse(Thread.currentThread().getContextClassLoader().getResourceAsStream(FUNCTIONS_CONFIG_XML));
    Object value =
        flowRunner("xpathWithDefaultNode").withVariable("xmlPayload", document).run().getMessage().getPayload().getValue();
    assertThat(value, is("xpathWithDefaultNode"));
  }

  @Test
  public void xpathWithOverrides() throws Exception {
    Document document = documentBuilderFactory.newDocumentBuilder()
        .parse(Thread.currentThread().getContextClassLoader().getResourceAsStream(FUNCTIONS_CONFIG_XML));
    Object value =
        flowRunner("xpathWithOverrides").withVariable("xmlPayload", document).run().getMessage().getPayload().getValue();
    assertThat(value, instanceOf(NodeList.class));
    assertThat(((NodeList) value).getLength(), is(8));
  }

}
