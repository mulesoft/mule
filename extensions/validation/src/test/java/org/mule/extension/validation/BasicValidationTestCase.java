/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.extension.validation.api.ValidationExtension.DEFAULT_LOCALE;
import static org.mule.extension.validation.internal.ImmutableValidationResult.error;
import org.mule.extension.validation.api.MultipleValidationException;
import org.mule.extension.validation.api.MultipleValidationResult;
import org.mule.extension.validation.api.ValidationResult;
import org.mule.extension.validation.api.Validator;
import org.mule.functional.junit4.FlowRunner;
import org.mule.mvel2.compiler.BlankLiteral;
import org.mule.runtime.core.api.MuleEvent;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class BasicValidationTestCase extends ValidationTestCase {

  private static final String CUSTOM_VALIDATOR_MESSAGE = "Do you wanna build a snowman?";
  private static final String EMAIL_VALIDATION_FLOW = "email";

  @Override
  protected String getConfigFile() {
    return "basic-validations.xml";
  }

  @Test
  public void email() throws Exception {
    assertValid(flowRunner(EMAIL_VALIDATION_FLOW).withPayload(VALID_EMAIL));
    assertInvalidEmail(INVALID_EMAIL);
    assertInvalidEmail(" " + VALID_EMAIL);
    assertInvalidEmail(VALID_EMAIL + " ");
  }

  @Test
  public void ip() throws Exception {
    assertValid(flowRunner("ip").withPayload("127.0.0.1"));
    assertValid(flowRunner("ip").withPayload("FE80:0000:0000:0000:0202:B3FF:FE1E:8329"));
    assertValid(flowRunner("ip").withPayload("FE80::0202:B3FF:FE1E:8329"));
    assertValid(flowRunner("ip").withPayload("0.0.0.0"));
    assertValid(flowRunner("ip").withPayload("0.0.0.1"));
    assertValid(flowRunner("ip").withPayload("10.0.0.0"));
    assertValid(flowRunner("ip").withPayload("192.168.0.0"));
    assertValid(flowRunner("ip").withPayload("172.16.0.0"));
    assertInvalid(flowRunner("ip").withPayload("1.1.256.0"), messages.invalidIp("1.1.256.0"));
    assertInvalid(flowRunner("ip").withPayload("0.0.0.a"), messages.invalidIp("0.0.0.a"));
    assertInvalid(flowRunner("ip").withPayload("12.1.2."), messages.invalidIp("12.1.2."));
    assertInvalid(flowRunner("ip").withPayload("192.168.100.0/24"), messages.invalidIp("192.168.100.0/24"));
    assertInvalid(flowRunner("ip").withPayload(0), messages.invalidIp("0"));
    String invalidIp = "12.1.2";
    assertInvalid(flowRunner("ip").withPayload(invalidIp), messages.invalidIp(invalidIp));
    invalidIp = "FE80:0000:0000";
    assertInvalid(flowRunner("ip").withPayload(invalidIp), messages.invalidIp(invalidIp));
  }

  @Test
  public void url() throws Exception {
    assertValid(flowRunner("url").withPayload(VALID_URL));
    assertValid(flowRunner("url")
        .withPayload("http://username:password@example.com:8042/over/there/index.dtb?type=animal&name=narwhal#nose"));
    assertInvalid(flowRunner("url").withPayload(INVALID_URL), messages.invalidUrl("here"));
  }

  @Test
  public void time() throws Exception {
    final String time = "12:08 PM";

    assertValid(configureTimeRunner(flowRunner("time"), time, "h:mm a"));
    assertValid(configureTimeRunner(flowRunner("time"), "Wed, Jul 4, '01", "EEE, MMM d, ''yy"));
    final String invalidPattern = "yyMMddHHmmssZ";
    assertInvalid(configureTimeRunner(flowRunner("time"), time, invalidPattern),
                  messages.invalidTime(time, DEFAULT_LOCALE, invalidPattern));
  }

  private FlowRunner configureTimeRunner(FlowRunner runner, String time, String pattern) {
    return runner.withPayload(time).withFlowVariable("pattern", pattern);
  }

  @Test
  public void matchesRegex() throws Exception {
    final String regex = "[tT]rue";

    FlowRunner runner =
        flowRunner("matchesRegex").withPayload("true").withFlowVariable("regexp", regex).withFlowVariable("caseSensitive", false);
    assertValid(runner);

    String testValue = "TRUE";
    assertValid(runner.withPayload(testValue));

    assertInvalid(runner.withFlowVariable("caseSensitive", true), messages.regexDoesNotMatch(testValue, regex));

    testValue = "tTrue";
    assertInvalid(runner.withPayload(testValue), messages.regexDoesNotMatch(testValue, regex));
  }

  @Test
  public void size() throws Exception {
    assertSize("abc");
    assertSize(Arrays.asList("a", "b", "c"));
    assertSize(new String[] {"a", "b", "c"});

    Map<String, String> map = new HashMap<>();
    map.put("a", "a");
    map.put("b", "b");
    map.put("c", "c");

    assertSize(map);
  }

  @Test
  public void isTrue() throws Exception {
    assertValid(flowRunner("isTrue").withPayload(true));
    assertInvalid(flowRunner("isTrue").withPayload(false), messages.failedBooleanValidation(false, true));
  }

  @Test
  public void isFalse() throws Exception {
    assertValid(flowRunner("isFalse").withPayload(false));
    assertInvalid(flowRunner("isFalse").withPayload(true), messages.failedBooleanValidation(true, false));
  }

  @Test
  public void notEmpty() throws Exception {
    final String flow = "notEmpty";

    assertValid(flowRunner(flow).withPayload("a"));
    assertValid(flowRunner(flow).withPayload(Arrays.asList("a")));
    assertValid(flowRunner(flow).withPayload(new String[] {"a"}));
    assertValid(flowRunner(flow).withPayload(ImmutableMap.of("a", "A")));
    assertInvalid(flowRunner(flow).withPayload(null), messages.valueIsNull());
    assertInvalid(flowRunner(flow).withPayload(""), messages.stringIsBlank());
    assertInvalid(flowRunner(flow).withPayload(ImmutableList.of()), messages.collectionIsEmpty());
    assertInvalid(flowRunner(flow).withPayload(new String[] {}), messages.arrayIsEmpty());
    assertInvalid(flowRunner(flow).withPayload(new Object[] {}), messages.arrayIsEmpty());
    assertInvalid(flowRunner(flow).withPayload(new int[] {}), messages.arrayIsEmpty());
    assertInvalid(flowRunner(flow).withPayload(new HashMap<String, String>()), messages.mapIsEmpty());
    assertInvalid(flowRunner(flow).withPayload(BlankLiteral.INSTANCE), messages.valueIsBlankLiteral());
  }

  @Test
  public void empty() throws Exception {
    final String flow = "empty";

    assertValid(flowRunner(flow).withPayload(""));
    assertValid(flowRunner(flow).withPayload(ImmutableList.of()));
    assertValid(flowRunner(flow).withPayload(new String[] {}));
    assertValid(flowRunner(flow).withPayload(new HashMap<String, String>()));
    assertInvalid(flowRunner(flow).withPayload("a"), messages.stringIsNotBlank());
    assertInvalid(flowRunner(flow).withPayload(Arrays.asList("a")), messages.collectionIsNotEmpty());
    assertInvalid(flowRunner(flow).withPayload(new String[] {"a"}), messages.arrayIsNotEmpty());
    assertInvalid(flowRunner(flow).withPayload(new Object[] {new Object()}), messages.arrayIsNotEmpty());
    assertInvalid(flowRunner(flow).withPayload(new int[] {0}), messages.arrayIsNotEmpty());
    assertInvalid(flowRunner(flow).withPayload(ImmutableMap.of("a", "a")), messages.mapIsNotEmpty());
  }

  @Test
  public void keepsPayloadWhenAllValidationsPass() throws Exception {
    FlowRunner runner = flowRunner("all");
    cofigureGetAllRunner(runner, VALID_EMAIL, VALID_URL);

    assertThat(runner.buildEvent().getMessage().getPayload(), is(sameInstance(runner.run().getMessage().getPayload())));
  }

  @Test
  public void twoFailuresInAllWithoutException() throws Exception {
    FlowRunner runner = flowRunner("all");
    cofigureGetAllRunner(runner, INVALID_EMAIL, INVALID_URL);
    Exception e = runner.runExpectingException();
    assertThat(e, is(instanceOf(MultipleValidationException.class)));
    MultipleValidationResult result = ((MultipleValidationException) e).getMultipleValidationResult();
    assertThat(result.getFailedValidationResults(), hasSize(2));
    assertThat(result.isError(), is(true));

    String expectedMessage = Joiner.on('\n').join(messages.invalidUrl(INVALID_URL), messages.invalidEmail(INVALID_EMAIL));

    assertThat(result.getMessage(), is(expectedMessage));

    for (ValidationResult failedValidationResult : result.getFailedValidationResults()) {
      assertThat(failedValidationResult.isError(), is(true));
    }
  }

  @Test
  public void oneFailInAll() throws Exception {
    FlowRunner runner = flowRunner("all");
    cofigureGetAllRunner(runner, INVALID_EMAIL, VALID_URL);
    Exception e = runner.runExpectingException();
    assertThat(e, is(instanceOf(MultipleValidationException.class)));
    MultipleValidationResult result = ((MultipleValidationException) e).getMultipleValidationResult();
    assertThat(result.getFailedValidationResults(), hasSize(1));
    assertThat(result.isError(), is(true));
    assertThat(result.getMessage(), is(messages.invalidEmail(INVALID_EMAIL).getMessage()));
  }

  @Test
  public void customValidationByClass() throws Exception {
    assertCustomValidator("customValidationByClass", CUSTOM_VALIDATOR_MESSAGE, CUSTOM_VALIDATOR_MESSAGE);
  }

  @Test
  public void customValidationByRef() throws Exception {
    assertCustomValidator("customValidationByRef", null, CUSTOM_VALIDATOR_MESSAGE);
  }

  @Test
  public void customValidatorWithCustomMessage() throws Exception {
    final String customMessage = "doesn't have to be a snowman";
    assertCustomValidator("customValidationByClass", customMessage, customMessage);
  }

  @Test
  public void usesValidatorAsRouter() throws Exception {
    final String flowName = "choice";

    assertThat(getPayloadAsString(flowRunner(flowName).withPayload(VALID_EMAIL).run().getMessage()), is("valid"));
    assertThat(getPayloadAsString(flowRunner(flowName).withPayload(INVALID_EMAIL).run().getMessage()), is("invalid"));
  }

  private void assertCustomValidator(String flowName, String customMessage, String expectedMessage) throws Exception {
    Exception e = flowRunner(flowName).withPayload("").withFlowVariable("customMessage", customMessage).runExpectingException();
    assertThat(e.getMessage(), is(expectedMessage));
  }

  private void cofigureGetAllRunner(FlowRunner runner, String email, String url) {
    runner.withPayload("").withFlowVariable("url", url).withFlowVariable(EMAIL_VALIDATION_FLOW, email);
  }

  private void assertInvalidEmail(String address) throws Exception {
    assertInvalid(flowRunner(EMAIL_VALIDATION_FLOW).withPayload(address), messages.invalidEmail(address));
  }

  private void assertSize(Object value) throws Exception {
    final String flowName = "size";
    final int expectedSize = 3;
    int minLength = 0;
    int maxLength = 3;

    assertValid(configureSizeValidationRunner(flowRunner(flowName), value, minLength, maxLength));
    minLength = 3;
    assertValid(configureSizeValidationRunner(flowRunner(flowName), value, minLength, maxLength));

    maxLength = 2;
    assertInvalid(configureSizeValidationRunner(flowRunner(flowName), value, minLength, maxLength),
                  messages.greaterThanMaxSize(value, maxLength, expectedSize));

    minLength = 5;
    maxLength = 10;
    assertInvalid(configureSizeValidationRunner(flowRunner(flowName), value, minLength, maxLength),
                  messages.lowerThanMinSize(value, minLength, expectedSize));
  }

  private FlowRunner configureSizeValidationRunner(FlowRunner runner, Object value, int minLength, int maxLength)
      throws Exception {
    runner.withPayload(value).withFlowVariable("minLength", minLength).withFlowVariable("maxLength", maxLength);

    return runner;
  }

  public static class TestCustomValidator implements Validator {

    @Override
    public ValidationResult validate(MuleEvent event) {
      return error(CUSTOM_VALIDATOR_MESSAGE);
    }
  }
}
