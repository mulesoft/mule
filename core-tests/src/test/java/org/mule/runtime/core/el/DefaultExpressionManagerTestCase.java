/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el;

import static java.lang.String.format;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.metadata.DataType.BYTE_ARRAY;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXPRESSION_LANGUAGE;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.el.ExpressionLanguage;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.el.context.MessageContext;
import org.mule.runtime.core.el.mvel.MVELExpressionLanguage;
import org.mule.runtime.core.metadata.DefaultTypedValue;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import com.mulesoft.weave.engine.exception.ExecutionException;

import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Expression Language")
@Stories("Support both MVEL and DW")
public class DefaultExpressionManagerTestCase extends AbstractMuleContextTestCase {

  private static final String MY_VAR = "myVar";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  ExtendedExpressionManager expressionManager;
  ExpressionLanguage mvelExpressionLanguage = new MVELExpressionLanguage(muleContext);

  @Before
  public void setUp() {
    MuleContext mockMuleContext = spy(muleContext);
    MuleRegistry registry = spy(muleContext.getRegistry());
    doReturn(registry).when(mockMuleContext).getRegistry();
    doReturn(mvelExpressionLanguage).when(registry).lookupObject(OBJECT_EXPRESSION_LANGUAGE);
    expressionManager = new DefaultExpressionManager(muleContext);
  }

  @Test
  @Description("Verifies that a simple literal expression is successful.")
  public void simple() {
    String expression = "\"wow\"";
    assertString(expression);
    assertString(weavify(expression));
  }

  @Test
  @Description("Verifies that a simple literal expression is successful when using brackets.")
  public void simpleEnclosed() {
    assertString("#[\"wow\"]");
    assertString("#[dw:\"wow\"]");
  }

  private void assertString(String expression) {
    assertThat(expressionManager.evaluate(expression).getValue(), is("wow"));
  }

  @Test
  @Description("Verifies that a map expression is successful.")
  public void map() {
    String expression = "{\'name\' : \'Sarah\', \'surname\' : \'Manning\'}";
    assertMap(expression);
    assertMap(weavify(expression));
  }

  private void assertMap(String expression) {
    Object result = expressionManager.evaluate(expression).getValue();
    assertThat(result, is(instanceOf(Map.class)));
    assertThat((Map<String, String>) result, hasEntry("name", "Sarah"));
    assertThat((Map<String, String>) result, hasEntry("surname", "Manning"));
  }

  @Test
  @Description("Verifies that custom variables are considered.")
  public void simpleCustomVariable() {
    Object object = new Object();
    BindingContext context = BindingContext.builder().addBinding(MY_VAR, new DefaultTypedValue(object, OBJECT)).build();
    assertThat(expressionManager.evaluate("#[myVar]", context).getValue(), equalTo(object));
    assertThat(expressionManager.evaluate("#[dw:myVar]", context).getValue(), equalTo(object));
  }

  @Test
  @Description("Verifies that the flow variable exposing it's name works.")
  public void flowName() throws MuleException {
    FlowConstruct mockFlowConstruct = mock(FlowConstruct.class);
    when(mockFlowConstruct.getName()).thenReturn("myFlowName");

    assertFlowName("#[flow.name]", mockFlowConstruct);
    assertFlowName("#[dw:flow.name]", mockFlowConstruct);
  }

  private void assertFlowName(String expression, FlowConstruct flowConstruct) throws MuleException {
    String result = (String) expressionManager.evaluate(expression, testEvent(), flowConstruct).getValue();
    assertThat(result, is(flowConstruct.getName()));
  }

  @Test
  @Description("Verifies that the Event variable still works for MVEL but that it fails for DW.")
  public void eventCompatibilityVariables() throws MuleException {
    Object mvelFlowResult = expressionManager.evaluate("#[_muleEvent]", testEvent()).getValue();
    assertThat(mvelFlowResult, is(instanceOf(Event.class)));

    expectedException.expect(RuntimeException.class);
    expressionManager.evaluate("#[dw:_muleEvent]", testEvent()).getValue();
  }

  @Test
  @Description("Verifies that the Message variable still works for MVEL but that it fails for DW.")
  public void messageCompatibilityVariables() throws MuleException {
    String expression = "message";
    Object mvelFlowResult = expressionManager.evaluate(expression, testEvent()).getValue();
    assertThat(mvelFlowResult, is(instanceOf(MessageContext.class)));

    expectedException.expect(ExecutionException.class);
    expressionManager.evaluate(weavify(expression), testEvent()).getValue();
  }

  @Test
  @Description("Verifies that payload variable works.")
  public void payloadVariable() throws MuleException {
    String payload = "payload";
    assertPayload(payload);
    assertPayload(weavify(payload));
  }

  private void assertPayload(String expression) throws MuleException {
    assertThat(expressionManager.evaluate(expression, testEvent()).getValue(), is(TEST_PAYLOAD));
  }

  @Test
  @Description("Verifies that flowVars work, returning null for non existent ones and it's value for those that do.")
  public void flowVars() throws MuleException {
    Event.Builder eventBuilder = Event.builder(testEvent());
    String mvelFlowVars = "flowVars.myVar";
    assertThat(expressionManager.evaluate(mvelFlowVars, eventBuilder.build()).getValue(), nullValue());
    String dwFlowVars = weavify("variables.myVar");
    assertThat(expressionManager.evaluate(dwFlowVars, eventBuilder.build()).getValue(), nullValue());
    String value = "Leda";
    eventBuilder.addVariable(MY_VAR, value);
    assertThat(expressionManager.evaluate(mvelFlowVars, eventBuilder.build()).getValue(), is(value));
    assertThat(expressionManager.evaluate(dwFlowVars, eventBuilder.build()).getValue(), is(value));
  }

  @Test
  @Description("Verifies that a simple transformation works.")
  public void transformation() throws MuleException {
    String expression = "payload";
    TypedValue mvelResult = expressionManager.evaluate(expression, BYTE_ARRAY, BindingContext.builder().build(), testEvent());
    assertThat(mvelResult.getValue(), is(TEST_PAYLOAD.getBytes()));
    assertThat(mvelResult.getDataType(), is(BYTE_ARRAY));
    TypedValue dwResult =
        expressionManager.evaluate(weavify(expression), BYTE_ARRAY, BindingContext.builder().build(), testEvent());
    assertThat(dwResult.getValue(), is(TEST_PAYLOAD.getBytes()));
    assertThat(dwResult.getDataType(), is(BYTE_ARRAY));
  }

  @Test
  @Description("Verifies that a simple transformation works even when it's not required.")
  public void transformationNotNeeded() throws MuleException {
    String expression = "payload";
    TypedValue mvelResult = expressionManager.evaluate(expression, STRING, BindingContext.builder().build(), testEvent());
    assertThat(mvelResult.getValue(), is(TEST_PAYLOAD));
    assertThat(mvelResult.getDataType(), is(STRING));
    TypedValue dwResult =
        expressionManager.evaluate(weavify(expression), STRING, BindingContext.builder().build(), testEvent());
    assertThat(dwResult.getValue(), is(TEST_PAYLOAD));
    assertThat(dwResult.getDataType(), is(STRING));
  }

  @Test
  @Description("Verifies that parsing works with inner expressions in MVEL but only with regular ones in DW.")
  public void parseCompatibility() throws MuleException {
    String expression = "this is #[payload]";
    assertThat(expressionManager.parse(expression, testEvent(), mock(FlowConstruct.class)),
               is(String.format("this is %s", TEST_PAYLOAD)));
    assertThat(expressionManager.parse("dw:'this is ' ++ payload", testEvent(), mock(FlowConstruct.class)),
               is(String.format("this is %s", TEST_PAYLOAD)));
    expectedException.expect(RuntimeException.class);
    expressionManager.parse(weavify(expression), testEvent(), mock(FlowConstruct.class));
  }

  @Test
  public void isValid() {
    String expression = "2*2";
    assertThat(expressionManager.isValid(expression), is(true));
    assertThat(expressionManager.isValid(weavify(expression)), is(true));
  }

  @Test
  public void isInvalid() {
    String expression = "2*'2";
    assertThat(expressionManager.isValid(expression), is(false));
    assertThat(expressionManager.isValid(weavify(expression)), is(false));
  }

  @Test
  public void isExpression() {
    assertThat(expressionManager.isExpression("2*2 + #[var]"), is(true));
    assertThat(expressionManager.isExpression("#[var]"), is(true));
    assertThat(expressionManager.isExpression("2*2 + #[dw:var]"), is(true));
    assertThat(expressionManager.isExpression("#[dw:var]"), is(true));

    assertThat(expressionManager.isExpression("${var}"), is(false));
    assertThat(expressionManager.isExpression("${dw:var}"), is(false));
  }

  @Test
  @Description("Verifies that enrichment using an Object only works for MVEL.")
  public void enrichObjectCompatibility() throws MuleException {
    Event event = testEvent();
    Event.Builder builder = Event.builder(event);
    FlowConstruct flowConstruct = mock(FlowConstruct.class);
    String myPayload = "myPayload";
    String expression = "payload";
    expressionManager.enrich(expression, event, builder, flowConstruct, myPayload);
    assertThat(builder.build().getMessage().getPayload().getValue(), is(myPayload));

    expectedException.expect(UnsupportedOperationException.class);
    expressionManager.enrich(weavify(expression), event, builder, flowConstruct, myPayload);
  }

  @Test
  @Description("Verifies that enrichment using a TypedValue only works for MVEL.")
  public void enrichTypedValueCompatibility() throws MuleException {
    Event event = testEvent();
    Event.Builder builder = Event.builder(event);
    FlowConstruct flowConstruct = mock(FlowConstruct.class);
    TypedValue myPayload = new DefaultTypedValue("myPayload", STRING);
    String expression = "payload";
    expressionManager.enrich(expression, event, builder, flowConstruct, myPayload);
    Event enrichedEvent = builder.build();
    assertThat(enrichedEvent.getMessage().getPayload().getValue(), is(myPayload.getValue()));
    assertThat(enrichedEvent.getMessage().getPayload().getDataType(), is(myPayload.getDataType()));

    expectedException.expect(UnsupportedOperationException.class);
    expressionManager.enrich(weavify(expression), event, builder, flowConstruct, myPayload);
  }

  private String weavify(String expression) {
    return format("dw:%s", expression);
  }

}
