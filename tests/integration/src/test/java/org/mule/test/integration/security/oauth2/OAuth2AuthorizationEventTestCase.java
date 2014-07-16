/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.security.oauth2;

import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.store.ObjectDoesNotExistException;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.security.oauth.BaseOAuth2Manager;
import org.mule.security.oauth.OAuth2Adapter;
import org.mule.security.oauth.OAuth2Manager;
import org.mule.security.oauth.OAuthProperties;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.store.InMemoryObjectStore;

import java.io.ByteArrayInputStream;
import java.io.Serializable;

import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OAuth2AuthorizationEventTestCase extends FunctionalTestCase
{

    private static final String payload = "this is the payload";
    private static final String eventId = "the event id";

    private MuleEvent event;
    private TestOAuth2Manager manager;
    private KeyedPoolableObjectFactory<String, OAuth2Adapter> objectFactory = null;
    private OAuth2Adapter adapter;
    private ObjectStore<Serializable> accessTokenObjectStore;
    private DefaultMuleMessage message;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp()
    {
        this.objectFactory = Mockito.mock(KeyedPoolableObjectFactory.class);
        this.adapter = Mockito.mock(
            OAuth2Adapter.class,
            Mockito.withSettings().extraInterfaces(Initialisable.class, Startable.class, Stoppable.class,
                Disposable.class, MuleContextAware.class));

        this.message = new DefaultMuleMessage(payload, muleContext);

        this.event = Mockito.mock(MuleEvent.class);
        Mockito.when(this.event.getId()).thenReturn(eventId);
        Mockito.when(this.event.getMessage()).thenReturn(message);

        this.accessTokenObjectStore = new InMemoryObjectStore<Serializable>();
        this.manager = Mockito.spy(new TestOAuth2Manager(this.objectFactory, this.adapter));
        this.manager.setAccessTokenObjectStore(this.accessTokenObjectStore);
    }

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/security/oauth/oauth2-authorization-event-test.xml";
    }

    @Test
    public void storeConsumableAuthorizationEvent() throws Exception
    {
        this.message.setPayload(new ByteArrayInputStream(payload.getBytes()));

        this.manager.storeAuthorizationEvent(event);

        Assert.assertEquals(message.getPayload(), payload);

        String key = String.format(OAuthProperties.AUTHORIZATION_EVENT_KEY_TEMPLATE, eventId);
        Assert.assertTrue(this.accessTokenObjectStore.contains(key));
        Assert.assertSame(this.accessTokenObjectStore.retrieve(key), event);
    }

    @Test
    public void storeSerializableAuthorizationEvent() throws Exception
    {
        this.manager.storeAuthorizationEvent(event);

        Assert.assertEquals(message.getPayload(), payload);

        String key = String.format(OAuthProperties.AUTHORIZATION_EVENT_KEY_TEMPLATE, eventId);
        Assert.assertTrue(this.accessTokenObjectStore.contains(key));
        Assert.assertSame(this.accessTokenObjectStore.retrieve(key), event);
    }

    @Test(expected = MessagingException.class)
    public void storeNotSerializableAuthorizationEvent() throws Exception
    {
        this.message.setPayload(new Object());
        this.manager.storeAuthorizationEvent(event);
    }

    @Test(expected = MessagingException.class)
    @SuppressWarnings("unchecked")
    public void storeAuthorizationEventOnFailingOS() throws Exception
    {
        ObjectStore<Serializable> failingOS = Mockito.mock(ObjectStore.class);
        Mockito.doThrow(new ObjectStoreException())
            .when(failingOS)
            .store(Mockito.any(Serializable.class), Mockito.any(Serializable.class));
        this.manager.setAccessTokenObjectStore(failingOS);
        this.manager.storeAuthorizationEvent(this.event);
    }

    @Test
    public void restoreAuthorizationEvent() throws Exception
    {
        this.storeSerializableAuthorizationEvent();
        MuleEvent restoredEvent = this.manager.restoreAuthorizationEvent(eventId);
        Assert.assertSame(restoredEvent, this.event);
    }

    @Test(expected = ObjectDoesNotExistException.class)
    public void restoreNotExistentAuthorizationEvent() throws Exception
    {
        this.manager.restoreAuthorizationEvent(eventId);
    }

    @Test(expected = ObjectStoreException.class)
    @SuppressWarnings("unchecked")
    public void restoreAuthorizationEventFromFailingObjectStore() throws Exception
    {
        ObjectStore<Serializable> failingOS = Mockito.mock(ObjectStore.class);
        Mockito.when(failingOS.retrieve(Mockito.any(Serializable.class))).thenThrow(
            new ObjectStoreException());
        this.manager.setAccessTokenObjectStore(failingOS);
        this.manager.restoreAuthorizationEvent(eventId);
    }
    
    private class TestOAuth2Manager extends BaseOAuth2Manager<OAuth2Adapter> {
        private final transient Logger logger = LoggerFactory.getLogger(TestOAuth2Manager.class);

        private KeyedPoolableObjectFactory<String, OAuth2Adapter> objectFactory;
        private OAuth2Adapter adapter;

        public TestOAuth2Manager(KeyedPoolableObjectFactory<String, OAuth2Adapter> objectFactory, OAuth2Adapter adapter)
        {
            this.objectFactory = objectFactory;
            this.adapter = adapter;
            this.setDefaultUnauthorizedConnector(this.adapter);
        }

        @Override
        protected Logger getLogger()
        {
            return logger;
        }

        @Override
        protected KeyedPoolableObjectFactory<String, OAuth2Adapter> createPoolFactory(OAuth2Manager<OAuth2Adapter> oauthManager,
                                                               ObjectStore<Serializable> objectStore)
        {
            return objectFactory;
        }

        @Override
        protected void fetchCallbackParameters(OAuth2Adapter adapter, String response)
        {
        }

        @Override
        protected void setCustomProperties(OAuth2Adapter adapter)
        {
        }

        @Override
        protected OAuth2Adapter instantiateAdapter()
        {
            return adapter;
        }

    }

}
