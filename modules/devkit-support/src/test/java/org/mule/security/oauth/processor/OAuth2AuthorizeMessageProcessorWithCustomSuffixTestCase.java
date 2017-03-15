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
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.security.oauth.OAuthProperties.BASE_EVENT_STATE_TEMPLATE;
import static org.mule.security.oauth.processor.OAuth2FetchAccessTokenProcessorWithCustomSuffixTestCase.STATE_AFTER_SEPARATOR;
import static org.mule.security.oauth.processor.OAuth2FetchAccessTokenProcessorWithCustomSuffixTestCase.STATE_BEFORE_SEPARATOR;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.callback.HttpCallback;
import org.mule.security.oauth.OAuth2Adapter;
import org.mule.security.oauth.OAuth2Manager;

import java.util.Map;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class OAuth2AuthorizeMessageProcessorWithCustomSuffixTestCase
{

    private static String CUSTOM_EVENT_STATE_TEMPLATE_SUFFIX = "__";

    private AuthorizeMessageProcessorOverrideSuffix authorizeMessageProcessorOverrideSuffix = new AuthorizeMessageProcessorOverrideSuffix();
    private MuleEvent event = mock(MuleEvent.class, RETURNS_DEEP_STUBS);
    private String processedState = null;

    @Test
    public void testOverrideTemplateSuffixThroughImplementedMethod() throws Exception
    {
        String customSuffix = authorizeMessageProcessorOverrideSuffix.getSuffix();
        assertThat(customSuffix, is(CUSTOM_EVENT_STATE_TEMPLATE_SUFFIX));
    }

    @Test
    public void testProcessWithImplementedMethod() throws Exception
    {
        final String expectedResult = format(BASE_EVENT_STATE_TEMPLATE + "%s%s", STATE_BEFORE_SEPARATOR, CUSTOM_EVENT_STATE_TEMPLATE_SUFFIX, STATE_AFTER_SEPARATOR);
        setMocks();
        authorizeMessageProcessorOverrideSuffix.doProcess(event);
        assertThat(processedState, is(expectedResult));
    }

    private void setMocks()
    {
        final OAuth2Manager manager = mock(OAuth2Manager.class, RETURNS_DEEP_STUBS);
        final HttpCallback callback = mock(HttpCallback.class, RETURNS_DEEP_STUBS);
        final MuleContext context = mock(MuleContext.class, RETURNS_DEEP_STUBS);
        when(context.getExpressionManager().parse(STATE_AFTER_SEPARATOR, event.getMessage())).thenReturn(STATE_AFTER_SEPARATOR);
        authorizeMessageProcessorOverrideSuffix.setOauthCallback(callback);
        authorizeMessageProcessorOverrideSuffix.setMuleContext(context);
        authorizeMessageProcessorOverrideSuffix.setModuleObject(manager);
        authorizeMessageProcessorOverrideSuffix.setState(STATE_AFTER_SEPARATOR);
        when(event.getId()).thenReturn(STATE_BEFORE_SEPARATOR);
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

    private class AuthorizeMessageProcessorOverrideSuffix extends
            BaseOAuth2AuthorizeMessageProcessor<OAuth2Manager<OAuth2Adapter>>
    {

        @Override
        protected Class<OAuth2Manager<OAuth2Adapter>> getOAuthManagerClass()
        {
            return null;
        }

        @Override
        protected String getSuffix()
        {
            return CUSTOM_EVENT_STATE_TEMPLATE_SUFFIX;
        }

        @Override
        protected String getAuthCodeRegex()
        {
            return null;
        }
    }

}
