/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.metadata.DataType.XML_STRING;
import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_JSON;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.streaming.bytes.InMemoryCursorStreamConfig;
import org.mule.runtime.core.api.streaming.bytes.InMemoryCursorStreamProvider;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.internal.streaming.bytes.SimpleByteBufferManager;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;

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
  public void echoFromManager() {
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
    assertThat(((NodeList) value).getLength(), is(10));
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

  @Test
  public void executeWithTypedValueParameters() throws Exception {
    final String xmlString = IOUtils.toString(getDocumentStream());
    final InputStream jsonStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("models/subtypes.json");
    final KnockeableDoor knockeableDoor = new KnockeableDoor("Ricky", "Universe 137");

    TypedValue<List<Object>> payload = flowRunner("typedValueFunction")
        .withPayload(new TypedValue<>(xmlString, XML_STRING))
        .withVariable("door", TypedValue.of(knockeableDoor))
        .withVariable("xmlString", new TypedValue<>(xmlString, XML_STRING))
        .withVariable("jsonStream", new TypedValue<>(jsonStream,
                                                     DataType.builder().type(InputStream.class).mediaType(APPLICATION_JSON)
                                                         .build()))
        .run().getMessage().getPayload();

    List<Object> values = payload.getValue();
    assertThat(values, hasSize(4));
    assertThat(getValue(values.get(0)), is(xmlString));
    assertThat(getValue(values.get(1)), is(xmlString));
    assertThat(getValue(values.get(2)), is(jsonStream));
    assertThat(getValue(values.get(3)), is(knockeableDoor));
  }

  @Test
  public void typedInputStream() throws Exception {
    String result = (String) flowRunner("typedInputStream").run().getMessage().getPayload().getValue();
    assertThat(result, containsString("employees"));
  }

  private InputStream getDocumentStream() {
    return Thread.currentThread().getContextClassLoader().getResourceAsStream(FUNCTIONS_CONFIG_XML);
  }

  private Object getValue(Object o) {
    return ((TypedValue) o).getValue();
  }

}
