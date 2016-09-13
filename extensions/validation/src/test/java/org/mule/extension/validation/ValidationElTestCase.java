/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.validation.ValidationTestCase.INVALID_EMAIL;
import static org.mule.extension.validation.ValidationTestCase.INVALID_URL;
import static org.mule.extension.validation.ValidationTestCase.VALID_EMAIL;
import static org.mule.extension.validation.ValidationTestCase.VALID_URL;

import org.mule.extension.validation.api.NumberType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.el.ExpressionLanguage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import com.google.common.collect.ImmutableList;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class ValidationElTestCase extends AbstractMuleContextTestCase {

  private ExpressionLanguage expressionLanguage;

  @Override
  protected void doSetUp() throws Exception {
    expressionLanguage = muleContext.getExpressionLanguage();
  }

  @Test
  public void email() throws Exception {
    final String expression = "#[validator.validateEmail(email)]";
    Event event = Event.builder(getTestEvent(""))
        .addVariable("email", VALID_EMAIL)
        .build();

    assertValid(expression, event);

    event = Event.builder(event).addVariable("email", INVALID_EMAIL).build();
    assertInvalid(expression, event);
  }

  @Test
  public void matchesRegex() throws Exception {
    final String regex = "[tT]rue";
    final String expression = "#[validator.matchesRegex(payload, regexp, caseSensitive)]";

    Event event = Event.builder(getTestEvent("true"))
        .addVariable("regexp", regex)
        .addVariable("caseSensitive", false)
        .build();

    assertValid(expression, event);

    event = Event.builder(event).message(InternalMessage.builder(event.getMessage()).payload("TRUE").build()).build();
    assertValid(expression, event);

    event = Event.builder(event).addVariable("caseSensitive", true).build();
    assertInvalid(expression, event);

    event = Event.builder(event).message(InternalMessage.builder(event.getMessage()).payload("tTrue").build()).build();
    assertInvalid(expression, event);
  }

  @Test
  public void isTime() throws Exception {
    final String time = "12:08 PM";

    Event event = Event.builder(getTestEvent(time))
        .addVariable("validPattern", "h:mm a")
        .addVariable("invalidPattern", "yyMMddHHmmssZ")
        .build();

    assertValid("#[validator.isTime(payload, validPattern)]", event);
    assertValid("#[validator.isTime(payload, validPattern, 'US')]", event);

    assertInvalid("#[validator.isTime(payload, invalidPattern)]", event);
    assertInvalid("#[validator.isTime(payload, invalidPattern, 'US')]", event);
  }

  @Test
  public void isEmpty() throws Exception {

    Map<String, String> map = new HashMap<>();

    assertEmpty("", true);
    assertEmpty(ImmutableList.of(), true);
    assertEmpty(new String[] {}, true);
    assertEmpty(map, true);
    assertEmpty("", true);

    map.put("a", "a");

    assertEmpty("a", false);
    assertEmpty(ImmutableList.of("a"), false);
    assertEmpty(new String[] {"a"}, false);
    assertEmpty(new Object[] {new Object()}, false);
    assertEmpty(new int[] {0}, false);
  }

  @Test
  public void notEmpty() throws Exception {

    Map<String, String> map = new HashMap<>();

    assertNotEmpty("", false);
    assertNotEmpty(ImmutableList.of(), false);
    assertNotEmpty(new String[] {}, false);
    assertNotEmpty(map, false);
    assertNotEmpty("", false);

    map.put("a", "a");

    assertNotEmpty("a", true);
    assertNotEmpty(ImmutableList.of("a"), true);
    assertNotEmpty(new String[] {"a"}, true);
    assertNotEmpty(new Object[] {new Object()}, true);
    assertNotEmpty(new int[] {0}, true);
  }

  @Test
  public void size() throws Exception {
    assertValid("#[validator.validateSize('John', 0, 4)]", getTestEvent(""));
    assertInvalid("#[validator.validateSize(payload, 1, 4)]", getTestEvent(ImmutableList.of()));
  }

  @Test
  public void notNull() throws Exception {
    final String expression = "#[validator.isNotNull(payload)]";
    assertValid(expression, getTestEvent(""));

    assertInvalid(expression, getTestEvent((InternalMessage.builder().nullPayload().build())));
  }

  @Test
  public void isNull() throws Exception {
    final String expression = "#[validator.isNull(payload)]";
    assertValid(expression, getTestEvent(InternalMessage.builder().nullPayload().build()));

    assertInvalid(expression, getTestEvent(""));
  }

  @Test
  public void isNumber() throws Exception {
    final String expression = "#[validator.isNumber(payload, numberType, minValue, maxValue)]";
    assertNumberValue(expression, NumberType.LONG, Long.MAX_VALUE / 2, Long.MIN_VALUE + 1, Long.MAX_VALUE - 1, Long.MIN_VALUE,
                      Long.MAX_VALUE);
    assertNumberValue(expression, NumberType.INTEGER, Integer.MAX_VALUE / 2, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1,
                      Integer.MIN_VALUE, Integer.MAX_VALUE);

    assertNumberValue(expression, NumberType.SHORT, new Short("100"), new Integer(Short.MIN_VALUE + 1).shortValue(),
                      new Integer(Short.MAX_VALUE - 1).shortValue(), Short.MIN_VALUE, Short.MAX_VALUE);
    assertNumberValue(expression, NumberType.DOUBLE, 10D, 1D, 10D, Double.MIN_VALUE, Double.MAX_VALUE);
    assertNumberValue(expression, NumberType.FLOAT, 10F, 1F, 10F, 0F, 20F);
  }

  @Test
  public void ip() throws Exception {
    final String expression = "#[validator.validateIp(payload)]";
    assertValid(expression, getTestEvent("127.0.0.1"));
    assertInvalid(expression, getTestEvent("ET phone home"));
  }

  @Test
  public void url() throws Exception {
    final String expression = "#[validator.validateUrl(payload)]";
    assertValid(expression, getTestEvent(VALID_URL));
    assertInvalid(expression, getTestEvent(INVALID_URL));
  }

  private <T extends Number> void assertNumberValue(String expression, NumberType numberType, T value, T minValue, T maxValue,
                                                    T lowerBoundaryViolation, T upperBoundaryViolation)
      throws Exception {
    assertValid(expression, getNumberValidationEvent(value, numberType, minValue, maxValue));
    final String invalid = "unparseable";
    assertInvalid(expression, getNumberValidationEvent(invalid, numberType, minValue, maxValue));

    assertInvalid(expression, getNumberValidationEvent(upperBoundaryViolation, numberType, minValue, maxValue));
    assertInvalid(expression, getNumberValidationEvent(lowerBoundaryViolation, numberType, minValue, maxValue));
  }

  private Event getNumberValidationEvent(Object value, NumberType numberType, Object minValue, Object maxValue)
      throws Exception {
    Event event = Event.builder(getTestEvent(value))
        .addVariable("numberType", numberType)
        .addVariable("minValue", minValue)
        .addVariable("maxValue", maxValue)
        .build();

    return event;
  }

  private void assertEmpty(Object value, boolean expected) throws Exception {
    testExpression("#[validator.isEmpty(payload)]", getTestEvent(value), expected);
  }

  private void assertNotEmpty(Object value, boolean expected) throws Exception {
    testExpression("#[validator.notEmpty(payload)]", getTestEvent(value), expected);
  }

  private boolean evaluate(String expression, Event event) {
    return expressionLanguage.evaluate(expression, event, null);
  }

  private void assertValid(String expression, Event event) {
    testExpression(expression, event, true);
  }

  private void assertInvalid(String expression, Event event) {
    testExpression(expression, event, false);
  }

  private void testExpression(String expression, Event event, boolean expected) {
    assertThat(evaluate(expression, event), is(expected));
  }
}
