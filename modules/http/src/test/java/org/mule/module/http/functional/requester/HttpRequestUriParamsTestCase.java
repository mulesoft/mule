/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.isA;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.rules.ExpectedException.none;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.transport.NullPayload;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 *
 */
public class HttpRequestUriParamsTestCase extends AbstractHttpRequestTestCase
{
    @Rule
    public ExpectedException expectedException = none();

    @Override
    protected String getConfigFile()
    {
        return "http-request-uri-params-config.xml";
    }

    @Test
    public void sendsUriParamsFromList() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("uriParamList");

        MuleEvent event = getTestEvent(TEST_MESSAGE);

        event.getMessage().setInvocationProperty("paramName", "testParam2");
        event.getMessage().setInvocationProperty("paramValue", "testValue2");

        flow.process(event);

        assertThat(uri, equalTo("/testPath/testValue1/testValue2"));
    }

    @Test
    public void sendsUriParamsFromMap() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("uriParamMap");

        MuleEvent event = getTestEvent(TEST_MESSAGE);

        Map<String, String> params = new HashMap<>();

        params.put("testParam1", "testValue1");
        params.put("testParam2", "testValue2");

        event.getMessage().setInvocationProperty("params", params);

        flow.process(event);

        assertThat(uri, equalTo("/testPath/testValue1/testValue2"));
    }

    @Test
    public void overridesUriParams() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("uriParamOverride");

        MuleEvent event = getTestEvent(TEST_MESSAGE);

        Map<String, String> params = new HashMap<>();

        params.put("testParam1", "testValueNew");
        params.put("testParam2", "testValue2");

        event.getMessage().setInvocationProperty("params", params);

        flow.process(event);

        assertThat(uri, equalTo("/testPath/testValueNew/testValue2"));
    }

    @Test
    public void sendsUriParamsIfNull() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("uriParamNull");

        MuleEvent event = getTestEvent(NullPayload.getInstance());

        expectedException.expect(MessagingException.class);
        expectedException.expectCause(isA(NullPointerException.class));
        expectedException.expectMessage(containsString("Expression {testParam2} evaluated to null."));
        flow.process(event);
    }

    @Test
    public void uriParamsContainsReservedUriCharacter() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("reservedUriCharacter");

        MuleEvent event = getTestEvent(TEST_PAYLOAD);

        event.getMessage().setInvocationProperty("paramName", "testParam");
        event.getMessage().setInvocationProperty("paramValue", "$a");

        flow.process(event);

        assertThat(uri, is("/testPath/$a"));
    }

    @Test
    public void uriParamsWithRegEx() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("regEx");

        MuleEvent event = getTestEvent(TEST_PAYLOAD);

        event.getMessage().setInvocationProperty("paramName", "[1-9]");
        event.getMessage().setInvocationProperty("paramValue", "abc");

        flow.process(event);

        assertThat(uri, is("/testPath/abc"));
    }

}
