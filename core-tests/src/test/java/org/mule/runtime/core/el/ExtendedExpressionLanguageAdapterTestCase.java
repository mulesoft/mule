/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el;

import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.api.metadata.DataType.fromFunction;
import static org.mule.runtime.core.api.Event.builder;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXPRESSION_LANGUAGE;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.EXPRESSION_LANGUAGE;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.ExpressionLanguageStory.SUPPORT_MVEL_DW;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionFunction;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.FunctionParameter;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.el.context.MessageContext;
import org.mule.runtime.core.el.mvel.MVELExpressionLanguage;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(EXPRESSION_LANGUAGE)
@Stories(SUPPORT_MVEL_DW)
@Description("Test cases verifying the differences between MVEL and DW ELs.")
public class ExtendedExpressionLanguageAdapterTestCase extends AbstractWeaveExpressionLanguageTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ExpressionLanguageAdaptorHandler expressionLanguageAdapter;
  private BindingContext emptyBindingContext = BindingContext.builder().build();

  @Before
  public void setUp() {
    super.setUp();
    MVELExpressionLanguage mvelExpressionLanguage = muleContext.getRegistry().lookupObject(OBJECT_EXPRESSION_LANGUAGE);
    expressionLanguageAdapter = new ExpressionLanguageAdaptorHandler(expressionLanguage, mvelExpressionLanguage);
  }

  @Test
  @Description("Verifies that global binding context only work for DW.")
  public void globalContext() throws Exception {
    ExpressionFunction expressionFunction = new ExpressionFunction() {

      private DataType dataType = STRING;

      @Override
      public Object call(Object[] objects, BindingContext bindingContext) {
        return ((String) objects[0]).toUpperCase();
      }

      @Override
      public Optional<DataType> returnType() {
        return Optional.of(dataType);
      }

      @Override
      public List<FunctionParameter> parameters() {
        return asList(new FunctionParameter("x", dataType));
      }
    };
    String global = "global";
    String value = "var";
    BindingContext context = BindingContext.builder()
        .addBinding(global, new TypedValue(value, STRING))
        .addBinding("upper", new TypedValue(expressionFunction, fromFunction(expressionFunction)))
        .build();
    expressionLanguageAdapter.addGlobalBindings(context);

    assertThat(expressionLanguageAdapter.evaluate(global, testEvent(), emptyBindingContext).getValue(), is(value));
    assertThat(expressionLanguageAdapter.evaluate("upper('hey')", testEvent(), emptyBindingContext).getValue(),
               is("HEY"));

    assertThat(expressionLanguageAdapter.evaluate(melify(global), testEvent(), context).getValue(), is(value));
    expectedException.expect(ExpressionRuntimeException.class);
    expressionLanguageAdapter.evaluate(melify(global), testEvent(), emptyBindingContext);
  }

  @Test
  @Description("Verifies that the Event variable still works for MVEL but that it fails for DW.")
  public void eventCompatibilityVariables() throws MuleException {
    String expression = "_muleEvent";
    Object mvelFlowResult = expressionLanguageAdapter.evaluate(melify(expression), testEvent(), emptyBindingContext).getValue();
    assertThat(mvelFlowResult, is(instanceOf(Event.class)));

    expectedException.expect(RuntimeException.class);
    expressionLanguageAdapter.evaluate(expression, testEvent(), emptyBindingContext).getValue();
  }

  @Test
  @Description("Verifies that the MuleContext variable still works for MVEL but that it fails for DW.")
  public void muleContextCompatibilityVariables() throws MuleException {
    String expression = "_muleContext";
    Object mvelFlowResult = expressionLanguageAdapter.evaluate(melify(expression), testEvent(), emptyBindingContext).getValue();
    assertThat(mvelFlowResult, is(instanceOf(MuleContext.class)));

    expectedException.expect(RuntimeException.class);
    expressionLanguageAdapter.evaluate(expression, testEvent(), emptyBindingContext).getValue();
  }

  @Test
  @Description("Verifies that the Message variable still works for MVEL but that it fails for DW.")
  public void messageCompatibilityVariables() throws MuleException {
    String expression = "message";
    Object mvelFlowResult = expressionLanguageAdapter.evaluate(melify(expression), testEvent(), emptyBindingContext).getValue();
    assertThat(mvelFlowResult, is(instanceOf(MessageContext.class)));

    expectedException.expect(ExpressionRuntimeException.class);
    expressionLanguageAdapter.evaluate(expression, testEvent(), emptyBindingContext).getValue();
  }

  @Test
  @Description("Verifies that the Flow name variable works for MVEL and DW.")
  public void flowNameVariable() throws MuleException {
    String expression = "flow.name";
    FlowConstruct mockFlowConstruct = mock(FlowConstruct.class);
    String myFlowName = "myFlowName";
    when(mockFlowConstruct.getName()).thenReturn(myFlowName);

    TypedValue mvelResult =
        expressionLanguageAdapter.evaluate(melify(expression), testEvent(), mockFlowConstruct, emptyBindingContext);
    assertThat(mvelResult.getValue(), is(myFlowName));

    TypedValue dwResult =
        expressionLanguageAdapter.evaluate(expression, testEvent(), mockFlowConstruct, emptyBindingContext);
    assertThat(dwResult.getValue(), is(myFlowName));

  }

  @Test
  @Description("Verifies that variables can be modified under MVEL but not DW.")
  public void variablesMutation() throws Exception {
    Event event = testEvent();
    Event.Builder builder1 = builder(event);
    TypedValue result = expressionLanguageAdapter.evaluate(melify("flowVars.put(\'key\',\'value\')"),
                                                           event,
                                                           builder1,
                                                           mock(FlowConstruct.class),
                                                           emptyBindingContext);
    assertThat(result.getValue(), is(nullValue()));
    assertThat(builder1.build().getVariableNames(), contains("key"));

    Event.Builder builder2 = builder(event);
    TypedValue result2 = expressionLanguageAdapter.evaluate("variables.put(\'key\',\'value\')",
                                                            event,
                                                            builder2,
                                                            mock(FlowConstruct.class),
                                                            emptyBindingContext);
    assertThat(result2.getValue(), is(nullValue()));
    assertThat(builder2.build().getVariableNames(), not(contains("key")));
  }

  @Test
  @Description("Verifies that the payload can be modified under MVEL but not DW.")
  public void payloadMutation() throws Exception {
    Event event = eventBuilder().message(of(1)).build();
    Event.Builder builder1 = builder(event);
    String expression = "payload = 3";
    TypedValue result = expressionLanguageAdapter.evaluate(melify(expression),
                                                           event,
                                                           builder1,
                                                           mock(FlowConstruct.class),
                                                           emptyBindingContext);
    assertThat(result.getValue(), is(1));
    assertThat(builder1.build().getMessage().getPayload().getValue(), is(3));
    Event.Builder builder2 = builder(event);

    expectedException.expect(ExpressionRuntimeException.class);
    expressionLanguageAdapter.evaluate(expression,
                                       event,
                                       builder2,
                                       mock(FlowConstruct.class),
                                       emptyBindingContext);
  }

  @Test
  @Description("Verifies that enrichment using an Object only works for MVEL.")
  public void enrichObjectCompatibility() throws MuleException {
    Event event = testEvent();
    Event.Builder builder = builder(event);
    FlowConstruct flowConstruct = mock(FlowConstruct.class);
    String myPayload = "myPayload";
    String expression = "payload";
    expressionLanguageAdapter.enrich(melify(expression), event, builder, flowConstruct, myPayload);
    assertThat(builder.build().getMessage().getPayload().getValue(), is(myPayload));

    expectedException.expect(UnsupportedOperationException.class);
    expressionLanguageAdapter.enrich(expression, event, builder, flowConstruct, myPayload);
  }

  @Test
  @Description("Verifies that enrichment using a TypedValue only works for MVEL.")
  public void enrichTypedValueCompatibility() throws MuleException {
    Event event = testEvent();
    Event.Builder builder = builder(event);
    FlowConstruct flowConstruct = mock(FlowConstruct.class);
    TypedValue myPayload = new TypedValue("myPayload", STRING);
    String expression = "payload";
    expressionLanguageAdapter.enrich(melify(expression), event, builder, flowConstruct, myPayload);
    Event enrichedEvent = builder.build();
    assertThat(enrichedEvent.getMessage().getPayload().getValue(), is(myPayload.getValue()));
    assertThat(enrichedEvent.getMessage().getPayload().getDataType(), is(myPayload.getDataType()));

    expectedException.expect(UnsupportedOperationException.class);
    expressionLanguageAdapter.enrich(expression, event, builder, flowConstruct, myPayload);
  }

  private String melify(String expression) {
    return format("mel:%s", expression);
  }

}
