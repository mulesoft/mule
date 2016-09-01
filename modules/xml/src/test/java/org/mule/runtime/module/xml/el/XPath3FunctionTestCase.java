/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.el;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.core.DefaultMuleEvent.setCurrentEvent;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.el.context.AbstractELTestCase;
import org.mule.runtime.module.xml.xpath.XPathReturnType;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.dom4j.DocumentHelper;
import org.junit.Test;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class XPath3FunctionTestCase extends AbstractELTestCase {

  private static final String ROOT_FOO_BAR = "<root foo=\"bar\"/>";
  private static final String BAR = "bar";

  public XPath3FunctionTestCase(AbstractELTestCase.Variant variant, String optimizer) {
    super(variant, optimizer);
  }

  @Test
  public void fromString() throws Exception {
    evaluateFooFromPayload(ROOT_FOO_BAR);
    evaluateFooFromFlowVar(ROOT_FOO_BAR);
  }

  @Test
  public void fromStream() throws Exception {
    InputStream payload = spy(new ByteArrayInputStream(ROOT_FOO_BAR.getBytes()));
    evaluateFooFromPayload(payload);

    verify(payload).close();

    // generate a new one since it was consumed
    payload = new ByteArrayInputStream(ROOT_FOO_BAR.getBytes());
    evaluateFooFromFlowVar(payload);
  }

  @Test
  public void messagePayloadChangedWhenPayloadConsumed() throws Exception {
    MuleEvent event = getTestEvent(new ByteArrayInputStream(ROOT_FOO_BAR.getBytes()));
    assertThat((String) doEvaluate("xpath3('/root/@foo')", event), equalTo(BAR));
    assertThat(event.getMessage().getPayload(), instanceOf(Node.class));
  }

  @Test
  public void returnTypes() throws Exception {
    Map<XPathReturnType, Class<?>> types = new HashMap<>();
    types.put(XPathReturnType.BOOLEAN, Boolean.class);
    types.put(XPathReturnType.NODE, Node.class);
    types.put(XPathReturnType.NODESET, NodeList.class);
    types.put(XPathReturnType.NUMBER, Double.class);
    types.put(XPathReturnType.STRING, String.class);

    MuleEvent event = getTestEvent(ROOT_FOO_BAR);
    for (Map.Entry<XPathReturnType, Class<?>> entry : types.entrySet()) {
      String expression = String.format("xpath3('/root/@foo', payload, '%s')", entry.getKey().name());
      assertThat(doEvaluate(expression, event), instanceOf(entry.getValue()));
    }
  }

  @Test(expected = ExpressionRuntimeException.class)
  public void noArgs() throws Exception {
    doEvaluate("xpath3()", getTestEvent(ROOT_FOO_BAR));
  }

  @Test(expected = ExpressionRuntimeException.class)
  public void tooManyArgs() throws Exception {
    doEvaluate("xpath3('/root/@foo', payload, 'STRING_DATA_TYPE', 'one too many')", getTestEvent(ROOT_FOO_BAR));
  }

  @Test(expected = ExpressionRuntimeException.class)
  public void blankExpression() throws Exception {
    doEvaluate("xpath3('')", getTestEvent(ROOT_FOO_BAR));
  }

  @Test(expected = ExpressionRuntimeException.class)
  public void notAStringExpression() throws Exception {
    doEvaluate("xpath3(System.out)", getTestEvent(ROOT_FOO_BAR));
  }

  @Test
  public void fromDom4jDocument() throws Exception {
    org.dom4j.Document document = DocumentHelper.parseText(ROOT_FOO_BAR);
    evaluateFooFromPayload(document);
    evaluateFooFromFlowVar(document);
  }

  @Test
  public void fromW3CDocument() throws Exception {
    org.w3c.dom.Document document = DocumentBuilderFactory.newInstance()
        .newDocumentBuilder()
        .parse(new InputSource(new StringReader(ROOT_FOO_BAR)));

    evaluateFooFromPayload(document);
    evaluateFooFromFlowVar(document);
  }

  @Test
  public void parametrized() throws Exception {
    MuleEvent event = getTestEvent(ROOT_FOO_BAR);
    event.setFlowVariable("foo", "bar");
    Object result = doEvaluate("xpath3('/root[@foo=$foo]', payload, 'NODE')", event);
    assertThat(result, instanceOf(Node.class));
    assertThat((((Node) result)).getAttributes().getNamedItem("foo").getNodeValue(), equalTo("bar"));
  }

  @Test
  public void emptyParametrizedResult() throws Exception {
    MuleEvent event = getTestEvent(ROOT_FOO_BAR);
    event.setFlowVariable("foo", "not a bar");
    Object result = doEvaluate("xpath3('/root[@foo=$foo]', payload, 'NODE')", event);
    assertThat(result, is(nullValue()));
  }

  @Test
  public void autoConvertNumericType() throws Exception {
    MuleEvent event = getTestEvent("<root foo=\"33\"/>");
    event.setFlowVariable("foo", 33);
    Object result = doEvaluate("xpath3('/root[@foo=$foo]', payload, 'NODE')", event);
    assertThat(result, instanceOf(Node.class));
    assertThat((((Node) result)).getAttributes().getNamedItem("foo").getNodeValue(), equalTo("33"));
  }

  private void evaluateFooFromPayload(Object payload) throws Exception {
    MuleMessage message = MuleMessage.builder().payload(payload).build();
    MuleEvent event = MuleEvent.builder(getTestEvent("")).message(message).build();

    assertThat((String) doEvaluate("xpath3('/root/@foo')", event), equalTo(BAR));
  }

  private void evaluateFooFromFlowVar(Object payload) throws Exception {
    MuleEvent event = getTestEvent("");
    event.setFlowVariable("input", payload);
    assertThat((String) doEvaluate("xpath3('/root/@foo', flowVars['input'])", event), equalTo(BAR));
  }

  private Object doEvaluate(String expression, MuleEvent event) throws Exception {

    setCurrentEvent(event);
    try {
      return evaluate(expression, event);
    } finally {
      setCurrentEvent(null);
    }
  }
}
