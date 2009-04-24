/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.api.routing.Router;
import org.mule.api.service.Service;
import org.mule.routing.inbound.ForwardingConsumer;
import org.mule.routing.inbound.IdempotentReceiver;
import org.mule.routing.inbound.IdempotentSecureHashReceiver;
import org.mule.routing.inbound.SelectiveConsumer;
import org.mule.tck.FunctionalTestCase;
import org.mule.util.store.InMemoryObjectStore;
import org.mule.util.store.TextFileObjectStore;

import java.util.List;

public class CoreNamespaceRoutersTestCase extends FunctionalTestCase
{
    public String getConfigResources()
    {
        return "core-namespace-routers.xml";
    }

    public void testForwardingRouter() throws Exception
    {
        Router r = lookupInboundRouterFromService("ForwardingRouter");
        assertTrue(r instanceof ForwardingConsumer);
    }

    public void testIdempotentSecureHashReceiverRouter() throws Exception
    {
        Router r = lookupInboundRouterFromService("IdempotentSecureHashReceiverRouter");
        assertTrue(r instanceof IdempotentSecureHashReceiver);
        IdempotentSecureHashReceiver router = (IdempotentSecureHashReceiver)r;
        assertEquals("SHA-128", router.getMessageDigestAlgorithm());
        assertNotNull(router.getStore());
        assertTrue(router.getStore() instanceof InMemoryObjectStore);
        InMemoryObjectStore store = (InMemoryObjectStore)router.getStore();
        assertEquals(1001, store.getEntryTTL());
        assertEquals(1001, store.getExpirationInterval());
        assertEquals(1001, store.getMaxEntries());
        assertEquals("xyz", store.getName());
        assertNotNull(store.getScheduler());
    }

     public void testIdempotentReceiverRouter() throws Exception
    {
        Router r = lookupInboundRouterFromService("IdempotentReceiverRouter");
        assertTrue(r instanceof IdempotentReceiver);
        IdempotentReceiver router = (IdempotentReceiver)r;
        assertEquals("#[message:id]-#[message:correlationId]", router.getIdExpression());
        assertNotNull(router.getStore());
        assertTrue(router.getStore() instanceof TextFileObjectStore);
        TextFileObjectStore store = (TextFileObjectStore)router.getStore();
        assertEquals(-1, store.getEntryTTL());
        assertEquals(1000, store.getExpirationInterval());
        assertEquals(10000000, store.getMaxEntries());
        assertEquals("foo", store.getDirectory());
        assertNotNull(store.getName());
        assertNotNull(store.getScheduler());
    }

    public void testSelectiveConsumerRouter() throws Exception
    {
        Router r = lookupInboundRouterFromService("SelectiveConsumerRouter");
        assertTrue(r instanceof SelectiveConsumer);
        assertFalse(((SelectiveConsumer) r).isTransformFirst());
    }

    protected Router lookupInboundRouterFromService(String serviceName) throws Exception
    {
        Service c = muleContext.getRegistry().lookupService(serviceName);
        assertNotNull(c);
        List routers = c.getInboundRouter().getRouters();
        assertEquals(1, routers.size());
        assertTrue(routers.get(0) instanceof Router);
        return (Router) routers.get(0);
    }
}
