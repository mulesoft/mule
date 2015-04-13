/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.extension.validation.internal.ValidationExtension.DEFAULT_LOCALE;
import org.mule.api.MuleEvent;
import org.mule.config.i18n.Message;
import org.mule.extension.validation.api.MultipleValidationException;
import org.mule.extension.validation.api.MultipleValidationResult;
import org.mule.extension.validation.api.ValidationException;
import org.mule.extension.validation.api.ValidationResult;
import org.mule.extension.validation.api.Validator;
import org.mule.extension.validation.internal.validator.CreditCardType;
import org.mule.mvel2.compiler.BlankLiteral;
import org.mule.util.ExceptionUtils;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class BasicValidationTestCase extends ValidationTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "basic-validations.xml";
    }

    @Test
    public void domain() throws Exception
    {
        assertValid("domain", getTestEvent("mulesoft.com"));
        assertInvalid("domain", getTestEvent("xxx.yy"), messages.invalidDomain("xxx.yy"));
    }

    @Test
    public void topLevelDomain() throws Exception
    {
        assertValid("topLevelDomain", getTestEvent("com"));
        assertInvalid("topLevelDomain", getTestEvent("abc"), messages.invalidTopLevelDomain("abc"));
    }

    @Test
    public void toplevelDomainCountryCode() throws Exception
    {
        assertValid("toplevelDomainCountryCode", getTestEvent("ar"));
        assertInvalid("toplevelDomainCountryCode", getTestEvent("ppp"), messages.invalidDomainCountryCode("ppp"));
    }

    @Test
    public void creditCardNumber() throws Exception
    {
        assertValid("creditCardNumber", getTestEvent(VALID_CREDIT_CARD_NUMBER));
        assertInvalid("creditCardNumber", getTestEvent(INVALID_CREDIT_CARD_NUMBER), messages.invalidCreditCard("5555444433332222", CreditCardType.MASTERCARD));
    }

    @Test
    public void email() throws Exception
    {
        assertValid("email", getTestEvent(VALID_EMAIL));
        assertInvalid("email", getTestEvent(INVALID_EMAIL), messages.invalidEmail("@mulesoft.com"));
    }

    @Test
    public void ip() throws Exception
    {
        assertValid("ip", getTestEvent("127.0.0.1"));
        assertInvalid("ip", getTestEvent("12.1.2"), messages.invalidIp("12.1.2"));
    }

    @Test
    public void isbn10() throws Exception
    {
        assertValid("isbn10", getTestEvent(VALID_ISBN10));
        assertInvalid("isbn10", getTestEvent(INVALID_ISBN10), messages.invalidISBN10("88"));
    }

    @Test
    public void isbn13() throws Exception
    {
        assertValid("isbn13", getTestEvent(VALID_ISBN13));
        assertInvalid("isbn13", getTestEvent(INVALID_ISBN13), messages.invalidISBN13("88"));
    }

    @Test
    public void url() throws Exception
    {
        assertValid("url", getTestEvent(VALID_URL));
        assertInvalid("url", getTestEvent(INVALID_URL), messages.invalidUrl("here"));
    }

    @Test
    public void time() throws Exception
    {
        final String time = "12:08 PM";
        MuleEvent event = getTestEvent(time);
        event.setFlowVariable("pattern", "h:mm a");
        assertValid("time", event);

        final String invalidPattern = "yyMMddHHmmssZ";
        event.setFlowVariable("pattern", invalidPattern);
        assertInvalid("time", event, messages.invalidTime(time, DEFAULT_LOCALE.toString(), invalidPattern));
    }

    @Test
    public void date() throws Exception
    {
        final String pattern = "yyyy-MM-dd";
        MuleEvent event = getTestEvent("1983-04-20");
        event.setFlowVariable("pattern", pattern);

        assertValid("date", event);

        final String invalidDate = "Wed, Jul 4, '01";
        event.getMessage().setPayload(invalidDate);
        assertInvalid("date", event, messages.invalidDate(invalidDate, DEFAULT_LOCALE.toString(), pattern));
    }

    @Test
    public void matchesRegex() throws Exception
    {
        final String regex = "[tT]rue";
        MuleEvent event = getTestEvent("true");
        event.setFlowVariable("regexp", regex);
        event.setFlowVariable("caseSensitive", false);

        assertValid("matchesRegex", event);

        String testValue = "TRUE";
        event.getMessage().setPayload(testValue);
        assertValid("matchesRegex", event);

        event.setFlowVariable("caseSensitive", true);
        assertInvalid("matchesRegex", event, messages.regexDoesNotMatch(testValue, regex));

        testValue = "tTrue";
        event.getMessage().setPayload(testValue);
        assertInvalid("matchesRegex", event, messages.regexDoesNotMatch(testValue, regex));
    }

    @Test
    public void size() throws Exception
    {
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
    public void isLong() throws Exception
    {
        assertNumberValue("long", Long.class, Long.MAX_VALUE / 2, Long.MIN_VALUE + 1, Long.MAX_VALUE - 1, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    @Test
    public void isInteger() throws Exception
    {
        assertNumberValue("integer", Integer.class, Integer.MAX_VALUE / 2, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    @Test
    public void isShort() throws Exception
    {
        assertNumberValue("short", Short.class, new Short("100"), new Integer(Short.MIN_VALUE + 1).shortValue(), new Integer(Short.MAX_VALUE - 1).shortValue(), Short.MIN_VALUE, Short.MAX_VALUE);
    }

    @Test
    public void isDouble() throws Exception
    {
        assertNumberValue("double", Double.class, 10D, 1D, 10D, Double.MIN_VALUE, Double.MAX_VALUE);
    }

    @Test
    public void isFloat() throws Exception
    {
        assertNumberValue("float", Float.class, 10F, 1F, 10F, 0F, 20F);
    }

    @Test
    public void isTrue() throws Exception
    {
        assertValid("isTrue", getTestEvent(true));
        assertInvalid("isTrue", getTestEvent(false), messages.failedBooleanValidation(false, true));
    }

    @Test
    public void isFalse() throws Exception
    {
        assertValid("isFalse", getTestEvent(false));
        assertInvalid("isFalse", getTestEvent(true), messages.failedBooleanValidation(true, false));
    }

    @Test
    public void notEmpty() throws Exception
    {
        final String flowName = "notEmpty";

        assertValid(flowName, getTestEvent("a"));
        assertValid(flowName, getTestEvent(Arrays.asList("a")));
        assertValid(flowName, getTestEvent(new String[] {"a"}));
        Map<String, String> map = new HashMap<>();
        map.put("a", "A");
        assertValid(flowName, getTestEvent(map));

        map.clear();
        assertInvalid(flowName, getTestEvent(null), messages.valueIsNull());
        assertInvalid(flowName, getTestEvent(""), messages.stringIsBlank());
        assertInvalid(flowName, getTestEvent(ImmutableList.of()), messages.collectionIsEmpty());
        assertInvalid(flowName, getTestEvent(new String[] {}), messages.arrayIsEmpty());
        assertInvalid(flowName, getTestEvent(new Object[] {}), messages.arrayIsEmpty());
        assertInvalid(flowName, getTestEvent(new int[] {}), messages.arrayIsEmpty());
        assertInvalid(flowName, getTestEvent(map), messages.mapIsEmpty());
        assertInvalid(flowName, getTestEvent(BlankLiteral.INSTANCE), messages.valueIsBlankLiteral());
    }

    @Test
    public void empty() throws Exception
    {
        final String flowName = "empty";

        assertValid(flowName, getTestEvent(""));
        assertValid(flowName, getTestEvent(ImmutableList.of()));
        assertValid(flowName, getTestEvent(new String[] {}));
        Map<String, String> map = new HashMap<>();
        assertValid(flowName, getTestEvent(map));

        assertInvalid(flowName, getTestEvent("a"), messages.stringIsNotBlank());
        assertInvalid(flowName, getTestEvent(Arrays.asList("a")), messages.collectionIsNotEmpty());
        assertInvalid(flowName, getTestEvent(new String[] {"a"}), messages.arrayIsNotEmpty());
        assertInvalid(flowName, getTestEvent(new Object[] {new Object()}), messages.arrayIsNotEmpty());
        assertInvalid(flowName, getTestEvent(new int[] {0}), messages.arrayIsNotEmpty());
        map.put("a", "a");
        assertInvalid(flowName, getTestEvent(map), messages.mapIsNotEmpty());
    }

    @Test
    public void successfulAll() throws Exception
    {
        MuleEvent responseEvent = runFlow("all", getAllEvent(VALID_EMAIL, VALID_CREDIT_CARD_NUMBER, true));
        assertThat(responseEvent.getMessage().getPayload(), is(instanceOf(MultipleValidationResult.class)));
        MultipleValidationResult result = (MultipleValidationResult) responseEvent.getMessage().getPayload();
        assertThat(result.isError(), is(false));
        assertThat(result.getFailedValidationResults(), hasSize(0));
    }

    @Test
    public void oneFailureInAllWithoutException() throws Exception
    {
        MuleEvent responseEvent = runFlow("all", getAllEvent(INVALID_EMAIL, VALID_CREDIT_CARD_NUMBER, false));
        assertThat(responseEvent.getMessage().getPayload(), is(instanceOf(MultipleValidationResult.class)));
        MultipleValidationResult result = (MultipleValidationResult) responseEvent.getMessage().getPayload();
        assertThat(result.isError(), is(true));
        assertThat(result.getMessage(), is(messages.invalidEmail(INVALID_EMAIL).getMessage()));
        assertThat(result.getFailedValidationResults(), hasSize(1));
        assertThat(result.getFailedValidationResults().get(0).isError(), is(true));
    }

    @Test
    public void twoFailureInAllWithoutException() throws Exception
    {
        MuleEvent responseEvent = runFlow("all", getAllEvent(INVALID_EMAIL, INVALID_CREDIT_CARD_NUMBER, false));
        assertThat(responseEvent.getMessage().getPayload(), is(instanceOf(ValidationResult.class)));
        MultipleValidationResult result = (MultipleValidationResult) responseEvent.getMessage().getPayload();
        assertThat(result.isError(), is(true));

        String expectedMessage = Joiner.on('\n').join(messages.invalidCreditCard(INVALID_CREDIT_CARD_NUMBER, CreditCardType.MASTERCARD),
                                                      messages.invalidEmail(INVALID_EMAIL));

        assertThat(result.getMessage(), is(expectedMessage));

        for (ValidationResult failedValidationResult : result.getFailedValidationResults())
        {
            assertThat(failedValidationResult.isError(), is(true));
        }
    }

    @Test
    public void failureInAllThrowsException() throws Exception
    {
        try
        {
            runFlow("all", getAllEvent(INVALID_EMAIL, VALID_CREDIT_CARD_NUMBER, true));
            fail("was expecting a failure");
        }
        catch (Exception e)
        {
            Throwable root = ExceptionUtils.getRootCause(e);
            assertThat(root, is(instanceOf(MultipleValidationException.class)));
            MultipleValidationResult result = ((MultipleValidationException) root).getMultipleValidationResult();
            assertThat(result.getFailedValidationResults(), hasSize(1));
            assertThat(result.isError(), is(true));
            assertThat(result.getMessage(), is(messages.invalidEmail(INVALID_EMAIL).getMessage()));
        }
    }

    @Test
    public void customValidationByClass() throws Exception
    {
        assertCustomValidator("customValidationByClass");
    }

    @Test
    public void customValidationByRef() throws Exception
    {
        assertCustomValidator("customValidationByRef");
    }

    private void assertCustomValidator(String flowName) throws Exception
    {
        try
        {
            runFlow(flowName, getTestEvent(""));
            fail("was expecting a failure");
        }
        catch (Exception e)
        {
            Throwable cause = ExceptionUtils.getRootCause(e);
            assertThat(CUSTOM_VALIDATOR_EXCEPTION, is(sameInstance(cause)));
        }
    }

    private MuleEvent getAllEvent(String email, String creditCardNumber, boolean throwsException) throws Exception
    {
        MuleEvent event = getTestEvent("");
        event.setFlowVariable("creditCardNumber", creditCardNumber);
        event.setFlowVariable("email", email);
        event.setFlowVariable("throwsException", throwsException);

        return event;
    }

    private <T extends Number> void assertNumberValue(String flowName,
                                                      Class<T> numberType,
                                                      T value,
                                                      T minValue,
                                                      T maxValue,
                                                      T lowerBoundaryViolation,
                                                      T upperBoundaryViolation) throws Exception
    {
        assertValid(flowName, getNumberValidationEvent(value, minValue, maxValue));
        final String invalid = "unparseable";
        assertInvalid(flowName, getNumberValidationEvent(invalid, minValue, maxValue), messages.invalidNumberType(invalid, numberType));

        assertInvalid(flowName, getNumberValidationEvent(upperBoundaryViolation, minValue, maxValue), messages.greaterThan(upperBoundaryViolation, maxValue));
        assertInvalid(flowName, getNumberValidationEvent(lowerBoundaryViolation, minValue, maxValue), messages.lowerThan(lowerBoundaryViolation, minValue));
    }

    private void assertSize(Object value) throws Exception
    {
        final String flowName = "size";
        final int expectedSize = 3;
        int minLength = 0;
        int maxLength = 3;

        assertValid(flowName, getSizeValidationEvent(value, minLength, maxLength));

        maxLength = 2;
        assertInvalid(flowName, getSizeValidationEvent(value, minLength, maxLength), messages.greaterThanMaxSize(value, maxLength, expectedSize));

        minLength = 5;
        maxLength = 10;
        assertInvalid(flowName, getSizeValidationEvent(value, minLength, maxLength), messages.lowerThanMinSize(value, minLength, expectedSize));
    }

    private MuleEvent getSizeValidationEvent(Object value, int minLength, int maxLength) throws Exception
    {
        MuleEvent event = getTestEvent(value);
        event.setFlowVariable("minLength", minLength);
        event.setFlowVariable("maxLength", maxLength);

        return event;
    }

    private MuleEvent getNumberValidationEvent(Object value, Object minValue, Object maxValue) throws Exception
    {
        MuleEvent event = getTestEvent(value);
        event.setFlowVariable("minValue", minValue);
        event.setFlowVariable("maxValue", maxValue);

        return event;
    }

    private void assertValid(String flowName, MuleEvent event) throws Exception
    {
        MuleEvent responseEvent = runFlow(flowName, event);
        assertThat(responseEvent.getMessage().getExceptionPayload(), is(nullValue()));
    }

    private void assertInvalid(String flowName, MuleEvent event, Message expectedMessage) throws Exception
    {
        try
        {
            runFlow(flowName, event);
            fail("Was expecting a failure");
        }
        catch (Exception e)
        {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            assertThat(rootCause, is(instanceOf(ValidationException.class)));
            assertThat(rootCause.getMessage(), is(expectedMessage.getMessage()));
            // assert that all placeholders were replaced in message
            assertThat(rootCause.getMessage(), not(containsString("${")));
        }
    }

    public static class TestCustomValidator implements Validator
    {

        @Override
        public ValidationResult validate(MuleEvent event)
        {
            throw CUSTOM_VALIDATOR_EXCEPTION;
        }
    }
}
