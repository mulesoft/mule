/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.security.oauth;

import static junit.framework.Assert.fail;
import static org.mockito.Mockito.mock;

import org.mule.api.MuleEvent;
import org.mule.api.store.ObjectStore;
import org.mule.api.transport.PropertyScope;
import org.mule.security.oauth.processor.OAuth2FetchAccessTokenMessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.store.InMemoryObjectStore;

import java.io.Serializable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.junit.Test;

public class FetchAccessTokenTestCase extends AbstractMuleContextTestCase implements Runnable
{

    private static final String accessToken = "MY_ACCESS_TOKEN";

    private MuleEvent event;
    private TestOAuth2Manager manager;
    private OAuth2FetchAccessTokenMessageProcessor processor;
    private OAuth2Adapter adapter;
    private ObjectStore<Serializable> objectStore;
    private CountDownLatch latch;
    private Exception exception;

    @Override
    @SuppressWarnings("unchecked")
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        this.adapter = mock(OAuth2Adapter.class);
        this.objectStore = new InMemoryObjectStore<Serializable>();

        this.event = getTestEvent("");
        event.getMessage().setProperty("state", "MULE_EVENT_ID=whatever>>", PropertyScope.INBOUND);
        this.latch = new CountDownLatch(1);
        this.exception = null;

        this.manager = new TestOAuth2Manager(mock(KeyedPoolableObjectFactory.class), adapter);
        this.manager.setAccessTokenObjectStore(this.objectStore);

        this.processor = new OAuth2FetchAccessTokenMessageProcessor(manager, accessToken);
        this.processor.setMuleContext(muleContext);
        this.processor.setAccessTokenId(accessToken);
    }

    /**
     * When the event is restored from a persistent object store everything works
     * perfectly. However, when using in memory object stores the reference to the
     * event's owner thread is kept and throws exception when its properties are
     * modified. For this reason, the event's access control needs to be reset when
     * restored. This tests verifies that this is so
     */
    @Test
    public void inMemoryObjectStore() throws Exception
    {
        this.objectStore.store("whatever-authorization-event", event);

        Thread t = new Thread(this);
        t.start();

        if (latch.await(1, TimeUnit.SECONDS))
        {
            if (this.exception != null)
            {
                throw this.exception;
            }
        }
        else
        {
            t.interrupt();
            fail("timeout");
        }
    }

    @Override
    public void run()
    {
        try
        {
            processor.process(event);
        }
        catch (Exception e)
        {
            this.exception = e;
        }
        finally
        {
            latch.countDown();
        }
    }

}
