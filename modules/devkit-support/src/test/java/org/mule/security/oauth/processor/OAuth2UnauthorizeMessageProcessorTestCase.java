/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.processor;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transformer.TransformerMessagingException;
import org.mule.security.oauth.OAuth2Adapter;
import org.mule.security.oauth.OAuth2Manager;
import org.mule.tck.size.SmallTest;

import java.lang.reflect.Type;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class OAuth2UnauthorizeMessageProcessorTestCase
{

    private static final String accessTokenId = "accessTokenId";

    private OAuth2Manager<OAuth2Adapter> manager;

    @Mock
    private OAuth2Adapter adapter;
    
    private MuleEvent event;

    private BaseOAuth2UnauthorizeMessageProcessor<OAuth2Manager<OAuth2Adapter>> processor;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp()
    {
        this.processor = new TestUnauthorizeMessageProcessor();
        this.processor.setAccessTokenId(accessTokenId);
        this.processor.setModuleObject(this.manager);
        
        this.manager = Mockito.mock(OAuth2Manager.class, Mockito.RETURNS_DEEP_STUBS);
        
        this.event = Mockito.mock(MuleEvent.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(this.event.getMessage().getPayload()).thenReturn("");
    }

    @Test
    public void unathorize() throws Exception
    {
        Mockito.when(this.manager.acquireAccessToken(accessTokenId)).thenReturn(this.adapter);
        this.processor.process(this.event);
        Mockito.verify(this.manager).destroyAccessToken(accessTokenId, adapter);
    }

    @Test(expected = DefaultMuleException.class)
    public void unathorizeNotExistent() throws Exception
    {
        Mockito.when(this.manager.acquireAccessToken(Mockito.anyString())).thenReturn(null);
        this.processor.process(this.event);
    }
    
    @Test
    public void idFromConfig() throws Exception {
        this.processor.setAccessTokenId(null);
        Mockito.when(this.manager.getDefaultUnauthorizedConnector().getName()).thenReturn(accessTokenId);
        this.unathorize();
    }

    private class TestUnauthorizeMessageProcessor extends
        BaseOAuth2UnauthorizeMessageProcessor<OAuth2Manager<OAuth2Adapter>>
    {

        @Override
        protected Class<OAuth2Manager<OAuth2Adapter>> getOAuthManagerClass()
        {
            return null;
        }
        
        @Override
        protected OAuth2Manager<OAuth2Adapter> getOAuthManager()
        {
            return manager;
        }

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
