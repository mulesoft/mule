/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.processor;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.callback.HttpCallback;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transformer.TransformerMessagingException;
import org.mule.security.oauth.OAuth1Adapter;
import org.mule.security.oauth.OAuthProperties;
import org.mule.security.oauth.notification.OAuthAuthorizeNotification;
import org.mule.tck.size.SmallTest;

import java.lang.reflect.Type;
import java.util.Map;

import junit.framework.Assert;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class OAuth1AuthorizeMessageProcessorTestCase
{

    private static final String redirectUri = "redirectUri";

    @Mock
    private OAuth1Adapter adapter;

    private TestAuthorizeMessageProcessor processor;
    private MuleEvent event;

    @Mock
    private MuleContext muleContext;

    @Before
    public void setUp()
    {
        this.event = Mockito.mock(MuleEvent.class, Mockito.RETURNS_DEEP_STUBS);

        HttpCallback callback = Mockito.mock(HttpCallback.class);
        Mockito.when(callback.getUrl()).thenReturn(redirectUri);

        this.processor = new TestAuthorizeMessageProcessor();
        this.processor.setOauthCallback(callback);
        this.processor.setModuleObject(this.adapter);
        this.processor.setMuleContext(this.muleContext);
    }

    @Test
    public void getAdapter()
    {
        Assert.assertSame(this.adapter, this.processor.getAdapter());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void process() throws Exception
    {
        final String state = "state";
        final String authorizeUrl = "authorizeUrl";
        final String accessTokenUrl = "accessTokenUrl";
        final String requestTokenUrl = "requestTokenUrl";
        final String location = "location";

        this.processor.setState(state);
        this.processor.setAuthorizationUrl(authorizeUrl);
        this.processor.setAccessTokenUrl(accessTokenUrl);
        this.processor.setRequestTokenUrl(requestTokenUrl);

        Mockito.when(
            this.adapter.authorize(Mockito.argThat(new BaseMatcher<Map<String, String>>()
            {
                @Override
                public boolean matches(Object item)
                {
                    Map<String, String> map = (Map<String, String>) item;
                    return state.equals(map.get("state"));
                }

                @Override
                public void describeTo(Description description)
                {
                    description.appendText("map matcher and asserter");

                }
            }), Mockito.eq(requestTokenUrl), Mockito.eq(accessTokenUrl), Mockito.eq(authorizeUrl),
                Mockito.eq(redirectUri))).thenReturn(location);

        this.processor.process(this.event);

        Mockito.verify(this.adapter).setAccessTokenUrl(accessTokenUrl);

        Mockito.verify(this.event.getMessage()).setOutboundProperty(OAuthProperties.HTTP_STATUS, "302");
        Mockito.verify(this.event.getMessage()).setOutboundProperty(OAuthProperties.CALLBACK_LOCATION,
            location);

        Mockito.verify(this.muleContext).fireNotification(
            Mockito.argThat(new OAuthNotificationMatcher(
                OAuthAuthorizeNotification.OAUTH_AUTHORIZATION_BEGIN, this.event)));
    }

    private class TestAuthorizeMessageProcessor extends BaseOAuth1AuthorizeMessageProcessor
    {

        @Override
        protected Class<? extends OAuth1Adapter> getAdapterClass()
        {
            return null;
        }

        @Override
        protected OAuth1Adapter getAdapter()
        {
            return super.getAdapter();
        }

        @Override
        protected String getAuthCodeRegex()
        {
            return "\"access_token\"[ ]*:[ ]*\"([^\\\"]*)\"";
        }

        /**
         * Ok, this is cheating, but actually executing the real implementation of
         * this method requires a lot of mocking for a low ROI since it will be
         * deeply tested on the integration tests
         */
        @Override
        protected Object evaluateAndTransform(MuleContext muleContext,
                                              MuleEvent event,
                                              Type expectedType,
                                              String expectedMimeType,
                                              Object source)
            throws TransformerException, TransformerMessagingException
        {
            return source != null ? source.toString() : null;
        }

        /**
         * Ok, this is cheating, but actually executing the real implementation of
         * this method requires a lot of mocking for a low ROI since it will be
         * deeply tested on the integration tests
         */
        @Override
        protected Object evaluateAndTransform(MuleContext muleContext,
                                              MuleMessage muleMessage,
                                              Type expectedType,
                                              String expectedMimeType,
                                              Object source)
            throws TransformerException, TransformerMessagingException
        {
            return source != null ? source.toString() : null;
        }
    }

}
