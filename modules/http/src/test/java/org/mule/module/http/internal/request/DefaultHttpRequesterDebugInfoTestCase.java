/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.internal.request;

import static java.lang.Boolean.TRUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mule.module.http.api.requester.HttpSendBodyMode.ALWAYS;
import static org.mule.module.http.internal.HttpParamType.QUERY_PARAM;
import static org.mule.module.http.internal.request.DefaultHttpRequester.AUTHENTICATION_TYPE_DEBUG;
import static org.mule.module.http.internal.request.DefaultHttpRequester.DOMAIN_DEBUG;
import static org.mule.module.http.internal.request.DefaultHttpRequester.FOLLOW_REDIRECTS_DEBUG;
import static org.mule.module.http.internal.request.DefaultHttpRequester.METHOD_DEBUG;
import static org.mule.module.http.internal.request.DefaultHttpRequester.PARSE_RESPONSE_DEBUG;
import static org.mule.module.http.internal.request.DefaultHttpRequester.PASSWORD_DEBUG;
import static org.mule.module.http.internal.request.DefaultHttpRequester.QUERY_PARAMS_DEBUG;
import static org.mule.module.http.internal.request.DefaultHttpRequester.RESPONSE_TIMEOUT_DEBUG;
import static org.mule.module.http.internal.request.DefaultHttpRequester.SECURITY_DEBUG;
import static org.mule.module.http.internal.request.DefaultHttpRequester.SEND_BODY_DEBUG;
import static org.mule.module.http.internal.request.DefaultHttpRequester.STREAMING_MODE_DEBUG;
import static org.mule.module.http.internal.request.DefaultHttpRequester.URI_DEBUG;
import static org.mule.module.http.internal.request.DefaultHttpRequester.USERNAME_DEBUG;
import static org.mule.module.http.internal.request.DefaultHttpRequester.WORKSTATION_DEBUG;
import static org.mule.tck.junit4.matcher.FieldDebugInfoMatcher.fieldLike;
import static org.mule.tck.junit4.matcher.ObjectDebugInfoMatcher.objectLike;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.debug.FieldDebugInfo;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.construct.Flow;
import org.mule.module.http.api.requester.HttpSendBodyMode;
import org.mule.module.http.internal.HttpParam;
import org.mule.module.http.internal.HttpSingleParam;
import org.mule.module.http.internal.domain.request.HttpRequestAuthentication;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

public class DefaultHttpRequesterDebugInfoTestCase extends AbstractMuleContextTestCase
{

    private static final String DOMAIN_PROPERTY = "domain";
    private static final String PASSWORD_PROPERTY = "password";
    private static final String PREEMPTIVE_PROPERTY = "preemptive";
    private static final String USERNAME_PROPERTY = "username";
    private static final String WORKSTATION_PROPERTY = "workstation";
    private static final String HOST_PROPERTY = "host";
    private static final String PORT_PROPERTY = "port";
    private static final String METHOD_PROPERTY = "method";
    private static final String STREAMING_MODE_PROPERTY = "streamingMode";
    private static final String SEND_BODY_PROPERTY = "sendBody";
    private static final String FOLLOW_REDIRECTS_PROPERTY = "followRedirects";
    private static final String PARSE_RESPONSE_PROPERTY = "parseResponse";
    private static final String RESPONSE_TIMEOUT_PROPERTY = "responseTimeout";

    private static final String DOMAIN = "myDomain";
    private static final String PASSWORD = "myPassword";
    private static final String USERNAME = "myUsername";
    private static final String WORKSTATION = "myWorkstation";
    private static final int RESPONSE_TIMEOUT = 5000;
    private static final String HOST = "myHost";
    private static final String PORT = "7777";
    private static final String METHOD = "GET";
    public static final String PARAM_NAME1 = "paramName1";
    public static final String PARAM_NAME2 = "paramName2";
    public static final String PARAM2_SECOND_VALUE_PROPERTY = PARAM_NAME2 + "_2";
    public static final String PARAM2_FIRST_VALUE_PROPERTY = PARAM_NAME2 + "_1";
    public static final String PARAM_VALUE1 = "foo";
    public static final String PARAM_VALUE2 = "bar";


    private DefaultHttpRequester requester = new DefaultHttpRequester();
    private DefaultHttpRequesterConfig config = new DefaultHttpRequesterConfig();
    private DefaultMuleMessage message;
    private DefaultMuleEvent event;

    @Before
    public void setup()
    {
        requester.setMuleContext(muleContext);
        config.setMuleContext(muleContext);
        requester.setConfig(config);
        requester.setPath("/");
        requester.setRequestBuilder(createRequestBuilder());

        message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        event = new DefaultMuleEvent(message, MessageExchangePattern.REQUEST_RESPONSE, mock(Flow.class));
    }

    @Test
    public void returnsDebugInfoWithSecurity() throws Exception
    {
        configureSecurityExpressions();
        addConfigSecurityProperties(message);

        doDebugInfoTest(message, event, getSecurityFieldsMatchers());
    }

    @Test
    public void returnsDebugInfoWithoutSecurity() throws Exception
    {
        doDebugInfoTest(message, event, null);
    }

    private void doDebugInfoTest(DefaultMuleMessage message, DefaultMuleEvent event, List<Matcher<FieldDebugInfo>> securityFieldMatchers) throws InitialisationException
    {
        configureRequesterExpressions();
        addRequesterProperties(message);

        final List<FieldDebugInfo> debugInfo = requester.getDebugInfo(event);

        assertThat(debugInfo.size(), equalTo(9));
        assertThat(debugInfo, hasItem(fieldLike(URI_DEBUG, String.class, String.format("http://%s:%s/", HOST, PORT))));
        assertThat(debugInfo, hasItem(fieldLike(METHOD_DEBUG, String.class, METHOD)));
        assertThat(debugInfo, hasItem(fieldLike(STREAMING_MODE_DEBUG, Boolean.class, TRUE)));
        assertThat(debugInfo, hasItem(fieldLike(SEND_BODY_DEBUG, HttpSendBodyMode.class, ALWAYS)));
        assertThat(debugInfo, hasItem(fieldLike(FOLLOW_REDIRECTS_DEBUG, Boolean.class, TRUE)));
        assertThat(debugInfo, hasItem(fieldLike(PARSE_RESPONSE_DEBUG, Boolean.class, TRUE)));
        assertThat(debugInfo, hasItem(fieldLike(RESPONSE_TIMEOUT_DEBUG, Integer.class, RESPONSE_TIMEOUT)));
        List<Matcher<FieldDebugInfo>> paramMatchers = new ArrayList<>();
        paramMatchers.add(fieldLike(PARAM_NAME1, String.class, PARAM_VALUE1));
        paramMatchers.add(fieldLike(PARAM_NAME2, List.class, contains(PARAM_VALUE1, PARAM_VALUE2)));
        assertThat(debugInfo, hasItem(objectLike(QUERY_PARAMS_DEBUG, List.class, paramMatchers)));

        if (securityFieldMatchers == null)
        {
            assertThat(debugInfo, hasItem(fieldLike(SECURITY_DEBUG, HttpRequestAuthentication.class, isNull())));
        }
        else
        {
            assertThat(debugInfo, hasItem(objectLike(SECURITY_DEBUG, HttpRequestAuthentication.class, securityFieldMatchers)));
        }
    }

    private List<Matcher<FieldDebugInfo>> getSecurityFieldsMatchers()
    {
        final List<Matcher<FieldDebugInfo>> securityFields = new ArrayList<>();
        securityFields.add(fieldLike(USERNAME_DEBUG, String.class, USERNAME));
        securityFields.add(fieldLike(DOMAIN_DEBUG, String.class, DOMAIN));
        securityFields.add(fieldLike(PASSWORD_DEBUG, String.class, PASSWORD));
        securityFields.add(fieldLike(WORKSTATION_DEBUG, String.class, WORKSTATION));
        securityFields.add(fieldLike(AUTHENTICATION_TYPE_DEBUG, String.class, "BASIC"));

        return securityFields;
    }

    private void addConfigSecurityProperties(DefaultMuleMessage message)
    {
        message.setInvocationProperty(DOMAIN_PROPERTY, DOMAIN);
        message.setInvocationProperty(PASSWORD_PROPERTY, PASSWORD);
        message.setInvocationProperty(PREEMPTIVE_PROPERTY, Boolean.FALSE.toString());
        message.setInvocationProperty(USERNAME_PROPERTY, USERNAME);
        message.setInvocationProperty(WORKSTATION_PROPERTY, WORKSTATION);
    }

    private void addRequesterProperties(DefaultMuleMessage message)
    {
        message.setInvocationProperty(HOST_PROPERTY, HOST);
        message.setInvocationProperty(PORT_PROPERTY, PORT);
        message.setInvocationProperty(METHOD_PROPERTY, METHOD);
        message.setInvocationProperty(STREAMING_MODE_PROPERTY, TRUE.toString());
        message.setInvocationProperty(SEND_BODY_PROPERTY, ALWAYS.toString());
        message.setInvocationProperty(FOLLOW_REDIRECTS_PROPERTY, TRUE.toString());
        message.setInvocationProperty(PARSE_RESPONSE_PROPERTY, TRUE.toString());
        message.setInvocationProperty(RESPONSE_TIMEOUT_PROPERTY, RESPONSE_TIMEOUT);
        message.setInvocationProperty(PARAM_NAME1, PARAM_VALUE1);
        message.setInvocationProperty(PARAM2_FIRST_VALUE_PROPERTY, PARAM_VALUE1);
        message.setInvocationProperty(PARAM2_SECOND_VALUE_PROPERTY, PARAM_VALUE2);
    }

    private void configureSecurityExpressions() throws InitialisationException
    {
        final DefaultHttpAuthentication authentication = new DefaultHttpAuthentication(HttpAuthenticationType.BASIC);
        authentication.setDomain(getExpression(DOMAIN_PROPERTY));
        authentication.setPassword(getExpression(PASSWORD_PROPERTY));
        authentication.setPreemptive(getExpression(PREEMPTIVE_PROPERTY));
        authentication.setUsername(getExpression(USERNAME_PROPERTY));
        authentication.setWorkstation(getExpression(WORKSTATION_PROPERTY));
        authentication.setMuleContext(muleContext);
        authentication.initialise();
        config.setAuthentication(authentication);
    }

    private String getExpression(String name)
    {
        return String.format("#[%s]", name);
    }

    private void configureRequesterExpressions() throws InitialisationException
    {
        requester.setHost(getExpression(HOST_PROPERTY));
        requester.setPort(getExpression(PORT_PROPERTY));
        requester.setMethod(getExpression(METHOD_PROPERTY));
        requester.setRequestStreamingMode(getExpression(STREAMING_MODE_PROPERTY));
        requester.setSendBodyMode(getExpression(SEND_BODY_PROPERTY));
        requester.setFollowRedirects(getExpression(FOLLOW_REDIRECTS_PROPERTY));
        requester.setFollowRedirects(getExpression(PARSE_RESPONSE_PROPERTY));
        requester.setResponseTimeout(getExpression(RESPONSE_TIMEOUT_PROPERTY));
        requester.initialise();
    }

    private HttpRequesterRequestBuilder createRequestBuilder()
    {
        final HttpRequesterRequestBuilder requestBuilder = new HttpRequesterRequestBuilder();

        List<HttpParam> params = new ArrayList<>();
        params.add(createHttpParam(PARAM_NAME1, getExpression(PARAM_NAME1)));
        params.add(createHttpParam(PARAM_NAME2, getExpression(PARAM2_FIRST_VALUE_PROPERTY)));
        params.add(createHttpParam(PARAM_NAME2, getExpression(PARAM2_SECOND_VALUE_PROPERTY)));
        requestBuilder.setParams(params);

        return requestBuilder;
    }

    private HttpSingleParam createHttpParam(String name, String value)
    {
        HttpSingleParam httpSingleParam = new HttpSingleParam(QUERY_PARAM);
        httpSingleParam.setName(name);
        httpSingleParam.setValue(value);
        httpSingleParam.setMuleContext(muleContext);

        return httpSingleParam;
    }
}
