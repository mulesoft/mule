/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el;

import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.EXPRESSION_LANGUAGE;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.ExpressionLanguageStory.SUPPORT_MVEL_DW;

import static java.lang.String.format;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.ByteArrayInputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(EXPRESSION_LANGUAGE)
@Story(SUPPORT_MVEL_DW)
public class DefaultExpressionManagerDefaultTestCase extends AbstractMuleContextTestCase {

  @Rule
  public MockitoRule mockitorule = MockitoJUnit.rule();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ExtendedExpressionManager expressionManager;

  @Before
  public void configureExpressionManager() throws MuleException {
    expressionManager = muleContext.getExpressionManager();
  }

  @Description("Verifies that parsing works with inner expressions in MVEL but only with regular ones in DW.")
  public void parseCompatibility() throws MuleException {
    assertThat(expressionManager.parse("#['this is ' ++ payload]", testEvent(), TEST_CONNECTOR_LOCATION),
               is(format("this is %s", TEST_PAYLOAD)));
  }

  @Test
  @Description("Verifies that parsing works for log template scenarios for both DW and MVEL.")
  public void parseLog() throws MuleException {
    assertThat(expressionManager.parseLogTemplate("this is #[payload]", testEvent(), TEST_CONNECTOR_LOCATION,
                                                  NULL_BINDING_CONTEXT),
               is(format("this is %s", TEST_PAYLOAD)));
  }

  @Test
  @Description("Verifies that streams are logged in DW but not in MVEL.")
  public void parseLogStream() throws MuleException {
    ByteArrayInputStream stream = new ByteArrayInputStream("hello".getBytes());
    CoreEvent event = getEventBuilder().message(of(stream)).build();
    assertThat(expressionManager.parseLogTemplate("this is #[payload]", event, TEST_CONNECTOR_LOCATION,
                                                  NULL_BINDING_CONTEXT),
               is("this is hello"));
  }

  @Test
  @Description("Verifies that parsing works for log template scenarios for both DW and MVEL.")
  public void parseLogValueWithExpressionMarkers() throws MuleException {
    String payloadWithExprMarkers = "#[hola]";
    assertThat(expressionManager.parseLogTemplate("this is #[payload]",
                                                  getEventBuilder().message(of(payloadWithExprMarkers)).build(),
                                                  TEST_CONNECTOR_LOCATION,
                                                  NULL_BINDING_CONTEXT),
               is(format("this is %s", payloadWithExprMarkers)));
  }
}
