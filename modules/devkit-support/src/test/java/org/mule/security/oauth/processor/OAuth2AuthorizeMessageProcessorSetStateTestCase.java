/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.security.oauth.processor;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.DefaultMuleContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.callback.HttpCallback;
import org.mule.security.oauth.OAuth2Adapter;
import org.mule.security.oauth.OAuth2Manager;

import java.util.Map;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class OAuth2AuthorizeMessageProcessorSetStateTestCase
{

    private TestAuthorizeMessageProcessor testAuthorizeMessageProcessor = new TestAuthorizeMessageProcessor();
    private MuleEvent event = mock(MuleEvent.class, RETURNS_DEEP_STUBS);
    private String processedState = null;
    private String eventId ;
    private final String otherId = "otherId";

    @Test
    public void testSetState() throws Exception
    {
        MuleContext context = new DefaultMuleContext();
        eventId = context.getUniqueIdString();
        final String expectedResult = eventId + otherId;
        setMocks();
        testAuthorizeMessageProcessor.doProcess(event);
        assertThat(processedState, is(expectedResult));
    }

    private void setMocks()
    {
        final OAuth2Manager manager = mock(OAuth2Manager.class, RETURNS_DEEP_STUBS);
        final HttpCallback callback = mock(HttpCallback.class, RETURNS_DEEP_STUBS);
        final MuleContext context = mock(MuleContext.class, RETURNS_DEEP_STUBS);
        when(context.getExpressionManager().parse(otherId, event.getMessage())).thenReturn(otherId);
        testAuthorizeMessageProcessor.setOauthCallback(callback);
        testAuthorizeMessageProcessor.setMuleContext(context);
        testAuthorizeMessageProcessor.setModuleObject(manager);
        testAuthorizeMessageProcessor.setState(otherId);
        when(event.getId()).thenReturn(eventId);
        doAnswer(buildAuthorizeUrlAnswer).when(manager).buildAuthorizeUrl(any(Map.class), any(String.class), any(String.class));
    }

    private Answer buildAuthorizeUrlAnswer = new Answer()
    {
        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable
        {
            Map<String, String> map = (Map) invocation.getArguments()[0];
            processedState = map.get("state");
            return null;
        }
    };

    private class TestAuthorizeMessageProcessor extends
            BaseOAuth2AuthorizeMessageProcessor<OAuth2Manager<OAuth2Adapter>>
    {

        @Override
        protected Class<OAuth2Manager<OAuth2Adapter>> getOAuthManagerClass()
        {
            return null;
        }

        @Override
        protected String getAuthCodeRegex()
        {
            return null;
        }
    }

}
