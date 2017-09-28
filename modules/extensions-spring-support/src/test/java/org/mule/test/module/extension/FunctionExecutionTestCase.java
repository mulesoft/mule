/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.streaming.bytes.InMemoryCursorStreamConfig;
import org.mule.runtime.core.api.streaming.bytes.InMemoryCursorStreamProvider;
import org.mule.tck.core.streaming.SimpleByteBufferManager;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.NodeList;

public class FunctionExecutionTestCase extends AbstractExtensionFunctionalTestCase {

  private static final String FUNCTIONS_CONFIG_XML = "functions-config.xml";
  private ExtendedExpressionManager expressionManager;

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
  }

  @Test
  public void echoFromManager() throws Exception {
    TypedValue result = expressionManager.evaluate("Fn::defaultPrimitives()");
    assertThat(result.getValue(), is("SUCCESS"));
  }

  @Test
  public void echoWithDefault() throws Exception {
    Object value = flowRunner("echoWithDefault").withPayload("sampleData")
        .run().getMessage().getPayload().getValue();
    assertThat(value, is("prefix_sampleData"));
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

    Object value = flowRunner("xpathWithDefaults")
        .withPayload(getDocumentStream()).run().getMessage()
        .getPayload().getValue();
    assertThat(value, is("xpathWithDefaults"));
  }

  @Test
  public void xpathWithDefaultNode() throws Exception {
    InputStream stream = getDocumentStream();
    InMemoryCursorStreamProvider streamProvider = new InMemoryCursorStreamProvider(
                                                                                   stream,
                                                                                   InMemoryCursorStreamConfig.getDefault(),
                                                                                   new SimpleByteBufferManager());
    Object value =
        flowRunner("xpathWithDefaultNode")
            .withVariable("xmlPayload", streamProvider)
            .run().getMessage().getPayload().getValue();
    assertThat(value, is("xpathWithDefaultNode"));
  }

  @Test
  public void xpathWithOverrides() throws Exception {
    Object value =
        flowRunner("xpathWithOverrides")
            .withVariable("xmlPayload", getDocumentStream())
            .run().getMessage().getPayload().getValue();
    assertThat(value, instanceOf(NodeList.class));
    assertThat(((NodeList) value).getLength(), is(7));
  }

  @Test
  public void executeAliasedFunctionName() throws Exception {
    TypedValue<List<List<Object>>> result = expressionManager.evaluate("Fn::partition([1,2,3,4,5,6,7,8], 3)");
    List<List<Object>> value = result.getValue();
    assertThat(value, hasSize(3));
    assertThat(value.get(0), hasSize(3));
    assertThat(value.get(1), hasSize(3));
    assertThat(value.get(2), hasSize(2));
  }

  private InputStream getDocumentStream() {
    return Thread.currentThread().getContextClassLoader().getResourceAsStream(FUNCTIONS_CONFIG_XML);
  }

}
