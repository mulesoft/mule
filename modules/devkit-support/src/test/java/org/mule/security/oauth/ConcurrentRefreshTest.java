/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.devkit.ProcessInterceptor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.filter.Filter;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.security.oauth.callback.ProcessCallback;
import org.mule.security.oauth.process.RefreshTokenProcessInterceptor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ConcurrentRefreshTest extends AbstractMuleContextTestCase
{

    private final String accessTokenId = "myToken";
    private final String tokenUrl = "tokenUrl";

    private MessageProcessor mp;
    private MuleEvent event;
    private OAuth2Adapter adapter;
    private ConcurrentProcessCallback callback;

    @Override
    protected ConfigurationBuilder getBuilder() throws Exception
    {
        return new SpringXmlConfigurationBuilder(new String[]{});
    }

    
    @Before
    public void setUp() throws Exception
    {
        this.mp = Mockito.mock(MessageProcessor.class);
        this.event = Mockito.mock(MuleEvent.class);
        this.adapter = Mockito.mock(OAuth2Adapter.class);
        this.callback = new ConcurrentProcessCallback(accessTokenId);

        Mockito.when(this.adapter.getRefreshToken()).thenReturn("someRefreshToken");
        Mockito.when(this.adapter.getAccessTokenUrl()).thenReturn(this.tokenUrl);

    }

    @Test
    public void concurrentRefresh() throws Exception
    {
        final int threadCount = 100;

        ProcessInterceptor<String, OAuth2Adapter> interceptor = new TestProcessInterceptor();
        final RefreshTokenProcessInterceptor<String> refresh = new RefreshTokenProcessInterceptor<String>(
            interceptor, muleContext);

        Mockito.doAnswer(new Answer<Void>()
        {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                callback.setRefreshed(true);
                return null;
            }
        }).when(adapter).refreshAccessToken(accessTokenId);

        final CountDownLatch latch = new CountDownLatch(threadCount);

        Runnable run = new Runnable()
        {

            @Override
            public void run()
            {
                try
                {
                    refresh.execute(callback, adapter, mp, event);
                    latch.countDown();
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        };

        for (int i = 0; i < threadCount; i++)
        {
            new Thread(run).start();
        }

        latch.await();

        Mockito.verify(adapter).refreshAccessToken(accessTokenId);
    }

    private class TestProcessInterceptor implements ProcessInterceptor<String, OAuth2Adapter>
    {

        @Override
        public String execute(ProcessCallback<String, OAuth2Adapter> callback,
                              OAuth2Adapter under,
                              Filter filter,
                              MuleMessage message) throws Exception
        {
            return null;
        }

        @Override
        public String execute(ProcessCallback<String, OAuth2Adapter> callback,
                              OAuth2Adapter under,
                              MessageProcessor messageProcessor,
                              MuleEvent event) throws Exception
        {

            callback.process(null);
            return null;
        }
    }

    private class ConcurrentProcessCallback extends ProcessCallback<String, OAuth2Adapter>
    {

        private int count = 0;
        private boolean refreshed = false;

        public ConcurrentProcessCallback(String accessTokenId)
        {
            this.setAccessTokenId(accessTokenId);
        }

        @Override
        public String process(OAuth2Adapter object) throws Exception
        {
            if (!refreshed && count >= 20)
            {
                throw new FileNotFoundException();
            }
            count++;
            return null;
        }

        @Override
        public List<Class<? extends Exception>> getManagedExceptions()
        {
            List<Class<? extends Exception>> list = new ArrayList<Class<? extends Exception>>();
            list.add(FileNotFoundException.class);

            return list;
        }

        @Override
        public boolean isProtected()
        {
            return true;
        }

        public void setRefreshed(boolean refreshed)
        {
            this.refreshed = refreshed;
        }
    }

}
