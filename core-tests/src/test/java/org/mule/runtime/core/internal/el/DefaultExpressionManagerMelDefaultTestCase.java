/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el;

import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.el.BindingContext.builder;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.metadata.DataType.BYTE_ARRAY;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.core.api.config.MuleProperties.COMPATIBILITY_PLUGIN_INSTALLED;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_MEL_AS_DEFAULT;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.EXPRESSION_LANGUAGE;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.ExpressionLanguageStory.SUPPORT_MVEL_DW;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(EXPRESSION_LANGUAGE)
@Story(SUPPORT_MVEL_DW)
@RunWith(MockitoJUnitRunner.class)
public class DefaultExpressionManagerMelDefaultTestCase extends AbstractMuleContextTestCase {

  private static final String MY_VAR = "myVar";

  @Rule
  public SystemProperty melDefault = new SystemProperty(MULE_MEL_AS_DEFAULT, TRUE.toString());

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ExtendedExpressionManager expressionManager;

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    Map<String, Object> objects = new HashMap<>();
    objects.putAll(super.getStartUpRegistryObjects());
    objects.put(COMPATIBILITY_PLUGIN_INSTALLED, new Object());
    return objects;
  }

  @Before
  public void configureExpressionManager() throws MuleException {
    expressionManager = new DefaultExpressionManager();
    initialiseIfNeeded(expressionManager, muleContext);
  }

  @Test
  @Description("Verifies that a simple literal expression is successful.")
  public void simple() {
    String expression = "\"wow\"";
    assertString(expression);
  }

  @Test
  @Description("Verifies that a simple literal expression is successful when using brackets.")
  public void simpleEnclosed() {
    assertString("#[\"wow\"]");
  }

  private void assertString(String expression) {
    assertThat(expressionManager.evaluate(expression).getValue(), is("wow"));
  }

  @Test
  @Description("Verifies that a map expression is successful.")
  public void map() {
    String expression = "{\'name\' : \'Sarah\', \'surname\' : \'Manning\'}";
    Object result = expressionManager.evaluate(expression).getValue();
    assertThat(result, is(instanceOf(Map.class)));
    assertThat((Map<String, String>) result, hasEntry("name", "Sarah"));
    assertThat((Map<String, String>) result, hasEntry("surname", "Manning"));
  }

  @Test
  @Description("Verifies that custom variables are considered.")
  public void simpleCustomVariable() {
    Object object = new Object();
    BindingContext context = builder().addBinding(MY_VAR, new TypedValue(object, OBJECT)).build();
    assertThat(expressionManager.evaluate("#[myVar]", context).getValue(), equalTo(object));
  }

  @Test
  @Description("Verifies that the flow variable exposing it's name works.")
  public void flowName() throws MuleException {
    FlowConstruct mockFlowConstruct = mock(FlowConstruct.class);
    when(mockFlowConstruct.getName()).thenReturn("myFlowName");

    String result = (String) expressionManager
        .evaluate("#[flow.name]", testEvent(), fromSingleComponent(mockFlowConstruct.getName())).getValue();
    assertThat(result, is(mockFlowConstruct.getName()));
  }

  @Test
  @Description("Verifies that payload variable works.")
  public void payloadVariable() throws MuleException {
    assertThat(expressionManager.evaluate("payload", testEvent()).getValue(), is(TEST_PAYLOAD));
  }

  @Test
  @Description("Verifies that flowVars work, returning null for non existent ones and it's value for those that do.")
  public void flowVars() throws MuleException {
    CoreEvent.Builder eventBuilder = CoreEvent.builder(testEvent());
    String flowVars = "flowVars.myVar";
    assertThat(expressionManager.evaluate(flowVars, eventBuilder.build()).getValue(), nullValue());
    String value = "Leda";
    eventBuilder.addVariable(MY_VAR, value);
    assertThat(expressionManager.evaluate(flowVars, eventBuilder.build()).getValue(), is(value));
  }

  @Test
  @Description("Verifies that a simple transformation works. MVEL ignores expectedDataType")
  public void transformation() throws MuleException {
    String expression = "payload";
    TypedValue result = expressionManager.evaluate(expression, BYTE_ARRAY, builder().build(), testEvent());
    assertThat(result.getValue(), is(TEST_PAYLOAD));
    assertThat(result.getDataType(), is(STRING));
  }

  @Test
  @Description("Verifies that a simple transformation works even when it's not required.")
  public void transformationNotNeeded() throws MuleException {
    String expression = "payload";
    TypedValue result = expressionManager.evaluate(expression, STRING, builder().build(), testEvent());
    assertThat(result.getValue(), is(TEST_PAYLOAD));
    assertThat(result.getDataType(), is(STRING));
  }

  @Test
  @Description("Verifies that parsing works with inner expressions in MVEL but only with regular ones in DW.")
  public void parseCompatibility() throws MuleException {
    assertThat(expressionManager.parse("this is #[payload]", testEvent(), TEST_CONNECTOR_LOCATION),
               is(format("this is %s", TEST_PAYLOAD)));
    assertThat(expressionManager.parse("#[dw:'this is ' ++ payload]", testEvent(), TEST_CONNECTOR_LOCATION),
               is(format("this is %s", TEST_PAYLOAD)));
  }

  @Test
  @Description("Verifies that parsing works for plain String scenarios.")
  public void parse() throws MuleException {
    String expression = "this is a test";
    assertThat(expressionManager.parse(expression, testEvent(), TEST_CONNECTOR_LOCATION), is(expression));
  }

  @Test
  @Description("Verifies that parsing works for log template scenarios for both DW and MVEL.")
  public void parseLog() throws MuleException {
    assertThat(expressionManager.parseLogTemplate("this is #[mel:payload]", testEvent(), TEST_CONNECTOR_LOCATION,
                                                  NULL_BINDING_CONTEXT),
               is(format("this is %s", TEST_PAYLOAD)));
    assertThat(expressionManager.parseLogTemplate("this is #[payload]", testEvent(), TEST_CONNECTOR_LOCATION,
                                                  NULL_BINDING_CONTEXT),
               is(format("this is %s", TEST_PAYLOAD)));
  }

  @Test
  @Description("Verifies that streams are logged in DW but not in MVEL.")
  public void parseLogStream() throws MuleException {
    ByteArrayInputStream stream = new ByteArrayInputStream("hello".getBytes());
    CoreEvent event = getEventBuilder().message(of(stream)).build();
    assertThat(expressionManager.parseLogTemplate("this is #[dw:payload]", event, TEST_CONNECTOR_LOCATION,
                                                  NULL_BINDING_CONTEXT),
               is("this is hello"));
    assertThat(expressionManager.parseLogTemplate("this is #[payload]", event, TEST_CONNECTOR_LOCATION,
                                                  NULL_BINDING_CONTEXT),
               both(startsWith("this is ")).and(containsString(stream.getClass().getSimpleName())));
  }

  @Test
  public void isValid() {
    String expression = "2*2";
    assertThat(expressionManager.isValid(expression), is(true));
  }

  @Test
  public void isInvalid() {
    String expression = "2*'2";
    assertThat(expressionManager.isValid(expression), is(false));
  }

  @Test
  public void isExpression() {
    assertThat(expressionManager.isExpression("2*2 + #[var]"), is(true));
    assertThat(expressionManager.isExpression("#[var]"), is(true));
    assertThat(expressionManager.isExpression("${var}"), is(false));
  }

  @Test
  @Description("Verifies that parsing works for log template scenarios for both DW and MVEL.")
  public void parseLogValueWithExpressionMarkers() throws MuleException {
    String payloadWithExprMarkers = "#[hola]";
    assertThat(expressionManager.parseLogTemplate("this is #[mel:payload]",
                                                  getEventBuilder().message(of(payloadWithExprMarkers)).build(),
                                                  TEST_CONNECTOR_LOCATION,
                                                  NULL_BINDING_CONTEXT),
               is(format("this is %s", payloadWithExprMarkers)));
    assertThat(expressionManager.parseLogTemplate("this is #[payload]",
                                                  getEventBuilder().message(of(payloadWithExprMarkers)).build(),
                                                  TEST_CONNECTOR_LOCATION,
                                                  NULL_BINDING_CONTEXT),
               is(format("this is %s", payloadWithExprMarkers)));
  }
}
