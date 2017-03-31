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
import static org.mule.runtime.api.message.Message.of;
import org.mule.extension.validation.api.NumberType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import com.google.common.collect.ImmutableList;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class ValidationElTestCase extends AbstractMuleContextTestCase {

  private ExpressionManager expressionManager;

  @Override
  protected void doSetUp() throws Exception {
    expressionManager = muleContext.getExpressionManager();
  }

  @Test
  public void email() throws Exception {
    final String expression = "#[mel:validator.validateEmail(email)]";
    Event event = eventBuilder().message(of("")).addVariable("email", VALID_EMAIL).build();

    assertValid(expression, event);

    event = Event.builder(event).addVariable("email", INVALID_EMAIL).build();
    assertInvalid(expression, event);
  }

  @Test
  public void matchesRegex() throws Exception {
    final String regex = "[tT]rue";
    final String expression = "#[mel:validator.matchesRegex(payload, regexp, caseSensitive)]";

    Event event = eventBuilder().message(of("true")).addVariable("regexp", regex)
        .addVariable("caseSensitive", false).build();

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

    Event event = Event.builder(eventBuilder()
        .message(of(time)).build())
        .addVariable("validPattern", "h:mm a")
        .addVariable("invalidPattern", "yyMMddHHmmssZ")
        .build();

    assertValid("#[mel:validator.isTime(payload, validPattern)]", event);
    assertValid("#[mel:validator.isTime(payload, validPattern, 'US')]", event);

    assertInvalid("#[mel:validator.isTime(payload, invalidPattern)]", event);
    assertInvalid("#[mel:validator.isTime(payload, invalidPattern, 'US')]", event);
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
    assertValid("#[mel:validator.validateSize('John', 0, 4)]", testEvent());
    assertInvalid("#[mel:validator.validateSize(payload, 1, 4)]",
                  eventBuilder().message(of(ImmutableList.of())).build());
  }

  @Test
  public void notNull() throws Exception {
    final String expression = "#[mel:validator.isNotNull(payload)]";
    assertValid(expression, testEvent());

    assertInvalid(expression, nullPayloadEvent());
  }

  @Test
  public void isNull() throws Exception {
    final String expression = "#[mel:validator.isNull(payload)]";
    assertValid(expression, nullPayloadEvent());

    assertInvalid(expression, testEvent());
  }

  @Test
  public void isNumber() throws Exception {
    final String expression = "#[mel:validator.isNumber(payload, numberType, minValue, maxValue)]";
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
    final String expression = "#[mel:validator.validateIp(payload)]";
    assertValid(expression, eventBuilder().message(of("127.0.0.1")).build());
    assertInvalid(expression, eventBuilder().message(of("ET phone home")).build());
  }

  @Test
  public void url() throws Exception {
    final String expression = "#[mel:validator.validateUrl(payload)]";
    assertValid(expression, eventBuilder().message(of(VALID_URL)).build());
    assertInvalid(expression, eventBuilder().message(of(INVALID_URL)).build());
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
    Event event = Event.builder(eventBuilder()
        .message(of(value)).build())
        .addVariable("numberType", numberType)
        .addVariable("minValue", minValue)
        .addVariable("maxValue", maxValue)
        .build();

    return event;
  }

  private void assertEmpty(Object value, boolean expected) throws Exception {
    testExpression("#[mel:validator.isEmpty(payload)]", eventBuilder().message(of(value)).build(), expected);
  }

  private void assertNotEmpty(Object value, boolean expected) throws Exception {
    testExpression("#[mel:validator.notEmpty(payload)]", eventBuilder().message(of(value)).build(), expected);
  }

  private boolean evaluate(String expression, Event event) {
    return (boolean) expressionManager.evaluate(expression, event).getValue();
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
