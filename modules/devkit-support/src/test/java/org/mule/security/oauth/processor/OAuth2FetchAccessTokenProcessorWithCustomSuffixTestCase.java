/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.security.oauth.processor;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.module.http.api.HttpConstants.RequestProperties.HTTP_QUERY_PARAMS;
import static org.mule.security.oauth.OAuthProperties.BASE_EVENT_STATE_TEMPLATE;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.module.http.internal.ParameterMap;
import org.mule.security.oauth.OAuth2Manager;
import org.mule.tck.size.SmallTest;


import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class OAuth2FetchAccessTokenProcessorWithCustomSuffixTestCase
{

    private static final String CUSTOM_PREFIX = "__";
    public static final String STATE_BEFORE_SEPARATOR = "stateBeforeSeparator";
    public static final String STATE_AFTER_SEPARATOR = "stateAfterSeparator";

    private final OAuth2Manager oAuth2Manager = mock(OAuth2Manager.class);
    private OAuth2FetchAccessTokenMessageProcessor oAuth2FetchAccessTokenMessageProcessor = new OAuth2FetchAccessTokenMessageProcessor(oAuth2Manager, "accessTokenId", CUSTOM_PREFIX);
    private MuleEvent event = mock(MuleEvent.class, RETURNS_DEEP_STUBS);
    private MuleEvent restoredEvent = mock(MuleEvent.class, RETURNS_DEEP_STUBS);
    private MuleMessage message = mock(MuleMessage.class);
    private String processedId = null;
    private ParameterMap parameterMap = new ParameterMap();
    private HashMap<String, String> returnMap = null;

    @Before
    public void setUp() throws Exception
    {
        String myState = format(BASE_EVENT_STATE_TEMPLATE + CUSTOM_PREFIX + "%s", STATE_BEFORE_SEPARATOR, STATE_AFTER_SEPARATOR);
        parameterMap.put("state", myState);
        when(event.getMessage().getInboundProperty("http.query.params")).thenReturn(parameterMap);
        when(restoredEvent.getMessage()).thenReturn(message);
        when(message.getInboundProperty("http.query.params")).thenReturn(new ParameterMap());
        doAnswer(answerToRestoreAuthorizationEvent).when(oAuth2Manager).restoreAuthorizationEvent(anyString());
        doAnswer(answerToSetProperty).when(message).setProperty(any(String.class), any(Map.class), any(PropertyScope.class));
    }

    @Test
    public void testRestoreOriginalEvent() throws Exception
    {
        oAuth2FetchAccessTokenMessageProcessor.restoreOriginalEvent(event);
        assertThat(processedId, is(STATE_BEFORE_SEPARATOR));
        assertThat(returnMap.get("state"), is(STATE_AFTER_SEPARATOR));
    }

    private Answer answerToRestoreAuthorizationEvent = new Answer()
    {
        @Override
        public Object answer(InvocationOnMock invocationOnMock) throws Throwable
        {
            processedId = (String) invocationOnMock.getArguments()[0];
            return restoredEvent;
        }
    };

    private Answer answerToSetProperty = new Answer()
    {
        @Override
        public Object answer(InvocationOnMock invocationOnMock) throws Throwable
        {
            if (invocationOnMock.getArguments()[0].equals(HTTP_QUERY_PARAMS))
            {
                returnMap = ((HashMap) invocationOnMock.getArguments()[1]);
            }
            return null;
        }
    };

}
