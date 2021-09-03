/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.dataweave;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.api.metadata.DataType.fromFunction;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXPRESSION_LANGUAGE;
import static org.mule.runtime.core.api.event.CoreEvent.builder;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.EXPRESSION_LANGUAGE;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.ExpressionLanguageStory.SUPPORT_MVEL_DW;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionFunction;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.FunctionParameter;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.el.ExpressionLanguageSessionAdaptor;
import org.mule.runtime.core.internal.el.context.MessageContext;
import org.mule.runtime.core.internal.el.mvel.MVELExpressionLanguage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.rmi.registry.Registry;
import java.util.List;
import java.util.Optional;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(EXPRESSION_LANGUAGE)
@Story(SUPPORT_MVEL_DW)
@DisplayName("MVEL vs. DW Expression Language")
public class ExtendedExpressionMvelLanguageAdapterTestCase extends AbstractMuleContextTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  protected Registry registry = mock(Registry.class);

  private MVELExpressionLanguage expressionLanguageAdapter;
  private final BindingContext emptyBindingContext = BindingContext.builder().build();

  @Before
  public void setUp() {
    expressionLanguageAdapter = ((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(OBJECT_EXPRESSION_LANGUAGE);
  }

  @Override
  protected boolean mockExprExecutorService() {
    return true;
  }

  @Test
  @Description("Verifies that global binding context only work for DW.")
  public void globalContext() throws Exception {
    ExpressionFunction expressionFunction = new ExpressionFunction() {

      private final DataType dataType = STRING;

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

    assertThat(expressionLanguageAdapter.evaluate(melify(global), testEvent(), context).getValue(), is(value));
    expectedException.expect(ExpressionRuntimeException.class);
    expressionLanguageAdapter.evaluate(melify(global), testEvent(), emptyBindingContext);
  }

  @Test
  @Description("Verifies that the Event variable still works for MVEL but that it fails for DW.")
  public void eventCompatibilityVariables() throws MuleException {
    String expression = "_muleEvent";
    Object mvelFlowResult = expressionLanguageAdapter.evaluate(melify(expression), testEvent(), emptyBindingContext).getValue();
    assertThat(mvelFlowResult, is(instanceOf(CoreEvent.class)));
  }

  @Test
  @Description("Verifies that the MuleContext variable still works for MVEL but that it fails for DW.")
  public void muleContextCompatibilityVariables() throws MuleException {
    String expression = "_muleContext";
    Object mvelFlowResult = expressionLanguageAdapter.evaluate(melify(expression), testEvent(), emptyBindingContext).getValue();
    assertThat(mvelFlowResult, is(instanceOf(MuleContext.class)));
  }

  @Test
  @Description("Verifies that the Message variable works for MVEL and DW.")
  public void messageCompatibilityVariables() throws MuleException {
    String expression = "message";
    Object mvelFlowResult = expressionLanguageAdapter.evaluate(melify(expression), testEvent(), emptyBindingContext).getValue();
    assertThat(mvelFlowResult, is(instanceOf(MessageContext.class)));
  }

  @Test
  @Description("Verifies that the Flow name variable works for MVEL and DW.")
  public void flowNameVariable() throws MuleException {
    String expression = "flow.name";
    String myFlowName = "myFlowName";

    TypedValue mvelResult =
        expressionLanguageAdapter.evaluate(melify(expression), testEvent(), fromSingleComponent(myFlowName), emptyBindingContext);
    assertThat(mvelResult.getValue(), is(myFlowName));
  }

  @Test
  @Description("Verifies that variables can be modified under MVEL but not DW.")
  public void variablesMutation() throws Exception {
    CoreEvent event = testEvent();
    CoreEvent.Builder builder1 = builder(event);
    TypedValue result = expressionLanguageAdapter.evaluate(melify("flowVars.put(\'key\',\'value\')"),
                                                           event,
                                                           builder1,
                                                           TEST_CONNECTOR_LOCATION,
                                                           emptyBindingContext);
    assertThat(result.getValue(), is(nullValue()));
    assertThat(builder1.build().getVariables().keySet(), contains("key"));
  }

  @Test
  @Description("Verifies that the payload can be modified under MVEL but not DW.")
  public void payloadMutation() throws Exception {
    CoreEvent event = eventBuilder(muleContext).message(of(1)).build();
    CoreEvent.Builder builder1 = builder(event);
    String expression = "payload = 3";
    TypedValue result = expressionLanguageAdapter.evaluate(melify(expression),
                                                           event,
                                                           builder1,
                                                           TEST_CONNECTOR_LOCATION,
                                                           emptyBindingContext);
    assertThat(result.getValue(), is(1));
    assertThat(builder1.build().getMessage().getPayload().getValue(), is(3));
    CoreEvent.Builder builder2 = builder(event);
  }

  private String melify(String expression) {
    return format("mel:%s", expression);
  }

  @Test
  @Description("Verifies that the Event variable still works for MVEL but that it fails for DW.")
  public void sessionEventCompatibilityVariables() throws MuleException {
    ExpressionLanguageSessionAdaptor session = expressionLanguageAdapter.openSession(null, testEvent(), emptyBindingContext);

    String expression = "_muleEvent";
    Object mvelFlowResult = session.evaluate(melify(expression)).getValue();
    assertThat(mvelFlowResult, is(instanceOf(CoreEvent.class)));
  }

  @Test
  @Description("Verifies that the Flow name variable works for MVEL and DW.")
  public void sessionFlowNameVariable() throws MuleException {
    String myFlowName = "myFlowName";
    ExpressionLanguageSessionAdaptor session =
        expressionLanguageAdapter.openSession(fromSingleComponent(myFlowName), testEvent(), emptyBindingContext);

    String expression = "flow.name";
    TypedValue mvelResult = session.evaluate(melify(expression));
    assertThat(mvelResult.getValue(), is(myFlowName));
  }
}
