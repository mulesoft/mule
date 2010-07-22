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

import org.mule.api.processor.MessageProcessor;
import org.mule.api.service.Service;
import org.mule.routing.IdempotentMessageFilter;
import org.mule.routing.IdempotentSecureHashMessageFilter;
import org.mule.routing.MessageFilter;
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

    public void testIdempotentSecureHashReceiverRouter() throws Exception
    {
        MessageProcessor r = lookupInboundRouterFromService("IdempotentSecureHashReceiverRouter");
        assertTrue(r instanceof IdempotentSecureHashMessageFilter);
        IdempotentSecureHashMessageFilter router = (IdempotentSecureHashMessageFilter)r;
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
        MessageProcessor r = lookupInboundRouterFromService("IdempotentReceiverRouter");
        assertTrue(r instanceof IdempotentMessageFilter);
        IdempotentMessageFilter router = (IdempotentMessageFilter)r;
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
        MessageProcessor r = lookupInboundRouterFromService("SelectiveConsumerRouter");
        assertTrue(r instanceof MessageFilter);
    }

    protected MessageProcessor lookupInboundRouterFromService(String serviceName) throws Exception
    {
        Service c = muleContext.getRegistry().lookupService(serviceName);
        assertNotNull(c);
        List routers = c.getMessageSource().getMessageProcessors();
        assertEquals(1, routers.size());
        assertTrue(routers.get(0) instanceof MessageProcessor);
        return (MessageProcessor) routers.get(0);
    }
}
