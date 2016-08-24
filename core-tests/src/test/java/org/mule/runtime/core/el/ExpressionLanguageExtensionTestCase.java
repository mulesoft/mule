/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.mvel2.compiler.AbstractParser;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.el.ExpressionLanguage;
import org.mule.runtime.core.api.el.ExpressionLanguageContext;
import org.mule.runtime.core.api.el.ExpressionLanguageExtension;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.config.builders.DefaultsConfigurationBuilder;
import org.mule.runtime.core.el.context.AbstractELTestCase;
import org.mule.runtime.core.el.context.AppContext;
import org.mule.runtime.core.el.mvel.MVELExpressionLanguage;
import org.mule.runtime.core.el.mvel.MVELExpressionLanguageContext;

import java.text.DateFormat;
import java.util.Calendar;

import org.junit.Assert;
import org.junit.Test;

public class ExpressionLanguageExtensionTestCase extends AbstractELTestCase {

  private String a = "hi";
  private String b = "hi";

  public ExpressionLanguageExtensionTestCase(Variant variant, String mvelOptimizer) {
    super(variant, mvelOptimizer);
  }

  @Override
  protected ConfigurationBuilder getBuilder() throws Exception {
    return new DefaultsConfigurationBuilder() {

      @Override
      public void configure(MuleContext muleContext) throws ConfigurationException {
        super.configure(muleContext);
        try {
          muleContext.getRegistry().registerObject("key1", new TestExtension());
        } catch (RegistrationException e) {
          throw new ConfigurationException(e);
        }
      }
    };
  }

  @Override
  protected ExpressionLanguage getExpressionLanguage() throws RegistrationException {
    MVELExpressionLanguage mvel = new MVELExpressionLanguage(muleContext);
    return mvel;
  }

  @Test
  public void importClass() throws RegistrationException, InitialisationException {
    assertThat(expressionLanguage.evaluate("Calendar"), equalTo(Calendar.class));
  }

  @Test
  public void importClassWithName() throws RegistrationException, InitialisationException {
    assertThat(expressionLanguage.evaluate("CAL"), equalTo(Calendar.class));
  }

  @Test
  public void importStaticMethod() throws RegistrationException, InitialisationException {
    assertThat(expressionLanguage.evaluate("dateFormat()"), is(DateFormat.getInstance()));
  }

  @Test
  public void variable() throws RegistrationException, InitialisationException {
    assertThat(expressionLanguage.evaluate("a"), is("hi"));
  }

  @Test
  public void assignValueToVariable() throws RegistrationException, InitialisationException {
    expressionLanguage.evaluate("a='1'");
  }

  @Test(expected = ExpressionRuntimeException.class)
  public void assignValueToFinalVariable() throws RegistrationException, InitialisationException {
    expressionLanguage.evaluate("final='1'");
  }

  @Test
  public void mutableVariable() throws RegistrationException, InitialisationException {
    assertThat(expressionLanguage.evaluate("b"), is("hi"));
  }

  @Test
  public void assignValueToMutableVariable() throws RegistrationException, InitialisationException {
    assertThat(expressionLanguage.evaluate("b='1'"), is("hi"));
    assertThat(b, is("1"));
  }

  @Test
  public void testShortcutVariable() throws RegistrationException, InitialisationException {
    MVELExpressionLanguage mvel = new MVELExpressionLanguage(muleContext);
    mvel.initialise();

    assertThat(mvel.evaluate("appShortcut.name"), is(muleContext.getConfiguration().getId()));
  }

  @Test
  public void testVariableAlias() throws Exception {
    MVELExpressionLanguage mvel = new MVELExpressionLanguage(muleContext);
    mvel.initialise();

    MuleEvent event = getTestEvent("foo");

    assertThat(mvel.evaluate("p", event, flowConstruct), is("foo"));
  }

  @Test
  public void testAssignValueToVariableAlias() throws Exception {
    MVELExpressionLanguage mvel = new MVELExpressionLanguage(muleContext);
    mvel.initialise();

    MuleEvent event = getTestEvent("");

    mvel.evaluate("p='bar'", event, flowConstruct);
    assertThat(event.getMessage().getPayload(), is("bar"));
  }

  @Test
  public void testMuleMessageAvailableAsVariable() throws Exception {
    MVELExpressionLanguage mvel = new MVELExpressionLanguage(muleContext);
    mvel.initialise();

    MuleEvent event = getTestEvent("");
    MuleMessage message = event.getMessage();
    mvel.evaluate("p=m.uniqueId", event, flowConstruct);
  }

  @Test
  public void testFunction() throws RegistrationException, InitialisationException {
    MVELExpressionLanguage mvel = new MVELExpressionLanguage(muleContext);
    mvel.initialise();

    assertThat(mvel.evaluate("f('one','two')"),
               is("called param[0]=one,param[1]=two,app.name=" + muleContext.getConfiguration().getId()));
  }

  @Test
  public void testMuleContextAvailableInFunction() throws RegistrationException, InitialisationException {
    MVELExpressionLanguage mvel = new MVELExpressionLanguage(muleContext);
    mvel.initialise();

    assertThat(mvel.evaluate("muleContext()"), is(muleContext));
  }

  @Test
  public void testMuleMessageAvailableInFunction() throws RegistrationException, InitialisationException {
    MVELExpressionLanguage mvel = new MVELExpressionLanguage(muleContext);
    mvel.initialise();

    MuleEvent event = mock(MuleEvent.class);
    MuleMessage message = mock(MuleMessage.class);
    when(event.getMessage()).thenReturn(message);

    Assert.assertEquals(message, mvel.evaluate("muleMessage()", event, flowConstruct));
  }

  @Test(expected = ExpressionRuntimeException.class)
  public void testFunctionInvalidParams() throws RegistrationException, InitialisationException {
    MVELExpressionLanguage mvel = new MVELExpressionLanguage(muleContext);
    mvel.initialise();
    mvel.evaluate("f('one')");
  }

  @Test
  public void testParserContextThreadLocalCleared() throws RegistrationException, InitialisationException {
    MVELExpressionLanguage mvel = new MVELExpressionLanguage(muleContext);
    mvel.initialise();
    // Ensure ParserContext ThreadLocal is cleared after initialization (occurs in deployment thread)
    assertThat(AbstractParser.contextControl(2, null, null), is(nullValue()));
    mvel.evaluate("f('one','two')");
    // Ensure ParserContext ThreadLocal is cleared after evaluation (occurs in receiver/flow/dispatcher thread)
    assertThat(AbstractParser.contextControl(2, null, null), is(nullValue()));
  }

  class TestExtension implements ExpressionLanguageExtension {

    @Override
    public void configureContext(ExpressionLanguageContext context) {
      context.importClass(Calendar.class);
      context.importClass("CAL", Calendar.class);
      try {
        context.importStaticMethod("dateFormat", DateFormat.class.getMethod("getInstance", new Class[] {}));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      context.addVariable("a", a);
      context.addVariable("b", b, (name, value, newValue) -> b = newValue);
      context.addAlias("appShortcut", "app");
      context.addFinalVariable("final", "final");
      context.addAlias("p", "message.payload");
      try {
        context.addAlias("m", "_muleMessage");
      } catch (Exception e) {
        // continue - test will fail.
      }
      context.declareFunction("f", (params, context1) -> "called param[0]=" + params[0] + ",param[1]=" + params[1] + ",app.name="
          + ((AppContext) context1.getVariable("app")).getName());
      context.declareFunction("muleContext", (params, context1) -> context1
          .getVariable(MVELExpressionLanguageContext.MULE_CONTEXT_INTERNAL_VARIABLE));
      context.declareFunction("muleMessage", (params, context1) -> context1
          .getVariable(MVELExpressionLanguageContext.MULE_MESSAGE_INTERNAL_VARIABLE));
    }
  }
}
