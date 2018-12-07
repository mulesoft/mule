/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.el.BindingContext.builder;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.metadata.DataType.BOOLEAN;
import static org.mule.runtime.api.metadata.DataType.BYTE_ARRAY;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.api.metadata.DataType.fromFunction;
import static org.mule.runtime.api.metadata.DataType.fromType;
import static org.mule.runtime.api.metadata.MediaType.XML;
import static org.mule.runtime.core.api.config.MuleProperties.COMPATIBILITY_PLUGIN_INSTALLED;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXPRESSION_LANGUAGE;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static org.mule.tck.junit4.matcher.IsEqualIgnoringLineBreaks.equalToIgnoringLineBreaks;
import static org.mule.tck.util.MuleContextUtils.mockMuleContext;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.EXPRESSION_LANGUAGE;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.ExpressionLanguageStory.SUPPORT_MVEL_DW;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.api.el.ExpressionFunction;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.FunctionParameter;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.el.ExpressionManagerSession;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.el.mvel.MVELExpressionLanguage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.weave.v2.el.WeaveDefaultExpressionLanguageFactoryService;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(EXPRESSION_LANGUAGE)
@Story(SUPPORT_MVEL_DW)
@RunWith(MockitoJUnitRunner.class)
public class DefaultExpressionManagerTestCase extends AbstractMuleContextTestCase {

  private static final String MY_VAR = "myVar";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private StreamingManager streamingManager;

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
  @Description("Verifies that global bindings can be added.")
  public void globals() {
    DataType integerType = fromType(Integer.class);

    ExpressionFunction multiply = new ExpressionFunction() {

      @Override
      public Object call(Object[] objects, BindingContext bindingContext) {
        return ((Integer) objects[0]) * ((Integer) objects[1]);
      }

      @Override
      public Optional<DataType> returnType() {
        return of(integerType);
      }

      @Override
      public List<FunctionParameter> parameters() {
        return asList(new FunctionParameter("x", integerType),
                      new FunctionParameter("y", integerType));
      }
    };

    BindingContext globalContext = builder()
        .addBinding("aNum", new TypedValue<>(4, fromType(Integer.class)))
        .addBinding("times", new TypedValue<>(multiply, fromFunction(multiply)))
        .build();

    expressionManager.addGlobalBindings(globalContext);

    TypedValue result = expressionManager.evaluate("aNum times 5");
    assertThat(result.getValue(), is(20));

    expressionManager.addGlobalBindings(builder().addBinding("otherNum", new TypedValue(3, integerType)).build());

    result = expressionManager.evaluate("(times(7, 3) + otherNum) / aNum");
    assertThat(result.getValue(), is(6));
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
    String flowVars = "vars.myVar";
    assertThat(expressionManager.evaluate(flowVars, eventBuilder.build()).getValue(), nullValue());
    String value = "Leda";
    eventBuilder.addVariable(MY_VAR, value);
    assertThat(expressionManager.evaluate(flowVars, eventBuilder.build()).getValue(), is(value));
  }

  @Test
  @Description("Verifies that a simple transformation works.")
  public void transformation() throws MuleException {
    String expression = "payload";
    TypedValue result = expressionManager.evaluate(expression, BYTE_ARRAY, builder().build(), testEvent());
    assertThat(result.getValue(), is(TEST_PAYLOAD.getBytes()));
    assertThat(result.getDataType(), is(BYTE_ARRAY));
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
  public void mvelWithNullBinding() throws MuleException {
    String expression = "#[mel: 2+2 ==4]";
    TypedValue result = expressionManager
        .evaluate(expression, BOOLEAN, builder().addBinding("payload", new TypedValue(null, OBJECT)).build(),
                  testEvent());
    assertThat(result.getValue(), is(true));
    assertThat(result.getDataType(), is(BOOLEAN));
  }

  @Test
  @Description("Verifies that parsing works with inner expressions in MVEL but only with regular ones in DW.")
  public void parseCompatibility() throws MuleException {
    assertThat(expressionManager.parse("this is #[mel:payload]", testEvent(), TEST_CONNECTOR_LOCATION),
               is(format("this is %s", TEST_PAYLOAD)));
    assertThat(expressionManager.parse("#['this is ' ++ payload]", testEvent(), TEST_CONNECTOR_LOCATION),
               is(format("this is %s", TEST_PAYLOAD)));
    expectedException.expect(RuntimeException.class);
    expressionManager.parse("this is #[payload]", testEvent(), TEST_CONNECTOR_LOCATION);
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
               is(String.format("this is %s", TEST_PAYLOAD)));
    assertThat(expressionManager.parseLogTemplate("this is #[payload]", testEvent(), TEST_CONNECTOR_LOCATION,
                                                  NULL_BINDING_CONTEXT),
               is(String.format("this is %s", TEST_PAYLOAD)));
  }

  @Test
  @Description("Verifies that parsing works for log template scenarios for both DW and MVEL using the message.")
  public void parseLogMessage() throws MuleException {
    String expectedOutput =
        "current message is \norg.mule.runtime.core.internal.message.DefaultMessageBuilder$MessageImplementation\n{"
            + "\n  payload=test\n  mediaType=*/*\n  attributes=<not set>\n  attributesMediaType=*/*\n}";
    assertThat(expressionManager.parseLogTemplate("current message is #[mel:message]", testEvent(), TEST_CONNECTOR_LOCATION,
                                                  NULL_BINDING_CONTEXT),
               is(equalToIgnoringLineBreaks(expectedOutput)));
    assertThat(expressionManager.parseLogTemplate("current message is #[message]", testEvent(), TEST_CONNECTOR_LOCATION,
                                                  NULL_BINDING_CONTEXT),
               is(equalToIgnoringLineBreaks(expectedOutput)));
  }

  @Test
  @Description("Verifies that XML content can be used for logging in DW.")
  public void parseLogXml() throws MuleException {
    CoreEvent event = getEventBuilder().message(Message.builder().value("<?xml version='1.0' encoding='US-ASCII'?>\n"
        + "<wsc_fields>\n"
        + "  <operation>echo</operation>\n"
        + "  <body_test>test</body_test>\n"
        + "</wsc_fields>")
        .mediaType(XML)
        .build())
        .build();
    assertThat(expressionManager.parseLogTemplate("this is #[payload.wsc_fields.operation]", event, TEST_CONNECTOR_LOCATION,
                                                  NULL_BINDING_CONTEXT),
               is("this is \"echo\""));
  }

  @Test
  @Description("Verifies that streams are logged in DW but not in MVEL.")
  public void parseLogStream() throws MuleException {
    ByteArrayInputStream stream = new ByteArrayInputStream("hello".getBytes());
    CoreEvent event = getEventBuilder().message(Message.of(stream)).build();
    assertThat(expressionManager.parseLogTemplate("this is #[payload]", event, TEST_CONNECTOR_LOCATION,
                                                  NULL_BINDING_CONTEXT),
               is("this is hello"));
    assertThat(expressionManager.parseLogTemplate("this is #[mel:payload]", event, TEST_CONNECTOR_LOCATION,
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
  public void managedCursor() throws Exception {
    final DefaultExpressionLanguageFactoryService mockFactory =
        mock(DefaultExpressionLanguageFactoryService.class, RETURNS_DEEP_STUBS);
    final ExpressionLanguage expressionLanguage = mock(ExpressionLanguage.class, RETURNS_DEEP_STUBS);
    final CursorProvider cursorProvider = mock(CursorProvider.class);

    Registry registry = mock(Registry.class);
    when(registry.lookupByType(DefaultExpressionLanguageFactoryService.class)).thenReturn(of(mockFactory));
    when(registry.lookupByName(OBJECT_EXPRESSION_LANGUAGE))
        .thenReturn(of(mock(MVELExpressionLanguage.class, RETURNS_DEEP_STUBS)));
    when(registry.lookupByName(COMPATIBILITY_PLUGIN_INSTALLED)).thenReturn(empty());

    TypedValue value = new TypedValue(cursorProvider, BYTE_ARRAY);
    when(expressionLanguage.evaluate(anyString(), any())).thenReturn(value);
    when(expressionLanguage.evaluate(anyString(), any(), any())).thenReturn(value);
    when(mockFactory.create()).thenReturn(expressionLanguage);

    expressionManager = new DefaultExpressionManager();
    ((DefaultExpressionManager) expressionManager).setRegistry(registry);
    ((DefaultExpressionManager) expressionManager).setStreamingManager(streamingManager);
    initialiseIfNeeded(expressionManager, false, muleContext);
    final CoreEvent event = testEvent();

    when(streamingManager.manage(cursorProvider, event.getContext())).thenReturn(cursorProvider);

    expressionManager.evaluate("someExpression", event);
    verify(streamingManager).manage(cursorProvider, event.getContext());
  }

  protected void disableMel() throws InitialisationException {
    Registry registry = mock(Registry.class);
    when(registry.lookupByType(DefaultExpressionLanguageFactoryService.class))
        .thenReturn(of(new WeaveDefaultExpressionLanguageFactoryService()));
    when(registry.lookupByName(COMPATIBILITY_PLUGIN_INSTALLED)).thenReturn(empty());

    final MuleContextWithRegistry mockMuleContext = mockMuleContext();
    MuleConfiguration config = mockMuleContext.getConfiguration();
    doReturn(true).when(config).isValidateExpressions();

    expressionManager = new DefaultExpressionManager();
    ((DefaultExpressionManager) expressionManager).setRegistry(registry);
    ((DefaultExpressionManager) expressionManager).setMuleContext(mockMuleContext);
    initialiseIfNeeded(expressionManager, false, mockMuleContext);
  }

  @Test
  public void melPrefixIsValidDw() throws MuleException {
    disableMel();

    assertThat(expressionManager.isValid("#[mel:1]"), is(true));
    expressionManager.evaluate("#[mel:1]");
  }

  @Test
  public void session() throws MuleException {
    Object object = new Object();
    BindingContext context = builder().addBinding(MY_VAR, new TypedValue(object, OBJECT)).build();

    ExpressionManagerSession session = expressionManager.openSession(context);

    assertThat(session.evaluate("#[myVar]").getValue(), equalTo(object));
  }
}
