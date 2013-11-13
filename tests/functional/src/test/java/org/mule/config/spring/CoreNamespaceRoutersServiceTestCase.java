/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.service.Service;
import org.mule.routing.IdempotentMessageFilter;
import org.mule.routing.IdempotentSecureHashMessageFilter;
import org.mule.routing.MessageFilter;
import org.mule.routing.outbound.AbstractOutboundRouter;
import org.mule.service.ServiceCompositeMessageSource;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.store.InMemoryObjectStore;
import org.mule.util.store.TextFileObjectStore;

import java.util.List;

import org.junit.Test;

public class CoreNamespaceRoutersServiceTestCase extends FunctionalTestCase
{
    @Override
    public String getConfigFile()
    {
        return "core-namespace-routers-service.xml";
    }

    @Test
    public void testIdempotentSecureHashReceiverRouter() throws Exception
    {
        MessageProcessor router = lookupInboundRouterFromService("IdempotentSecureHashReceiverRouter");
        assertTrue(router instanceof IdempotentSecureHashMessageFilter);

        IdempotentSecureHashMessageFilter filter = (IdempotentSecureHashMessageFilter)router;
        assertEquals("SHA-128", filter.getMessageDigestAlgorithm());
        assertNotNull(filter.getStore());
        assertTrue(filter.getStore() instanceof InMemoryObjectStore);

        InMemoryObjectStore<String> store = (InMemoryObjectStore<String>)filter.getStore();
        assertEquals(1001, store.getEntryTTL());
        assertEquals(1001, store.getExpirationInterval());
        assertEquals(1001, store.getMaxEntries());
        assertEquals("xyz", store.getName());
        assertNotNull(store.getScheduler());
    }

    @Test
    public void testIdempotentReceiverRouter() throws Exception
    {
        MessageProcessor router = lookupInboundRouterFromService("IdempotentReceiverRouter");
        assertTrue(router instanceof IdempotentMessageFilter);

        IdempotentMessageFilter filter = (IdempotentMessageFilter)router;
        assertEquals("#[message:id]-#[message:correlationId]", filter.getIdExpression());
        assertNotNull(filter.getStore());
        assertTrue(filter.getStore() instanceof TextFileObjectStore);

        TextFileObjectStore store = (TextFileObjectStore)filter.getStore();
        assertEquals(-1, store.getEntryTTL());
        assertEquals(1000, store.getExpirationInterval());
        assertEquals(10000000, store.getMaxEntries());
        assertEquals("foo", store.getDirectory());
        assertNotNull(store.getName());
        assertNotNull(store.getScheduler());
    }

    @Test
    public void testSelectiveConsumerRouter() throws Exception
    {
        MessageProcessor router = lookupInboundRouterFromService("SelectiveConsumerRouter");
        assertTrue(router instanceof MessageFilter);
    }

    @Test
    public void testCustomRouter() throws Exception
    {
        MessageProcessor router = lookupOutboundRouterFromService("CustomRouter");
        assertTrue(router instanceof CustomOutboundRouter);
    }

    protected MessageProcessor lookupOutboundRouterFromService(String serviceName) throws Exception
    {
        Service service = lookupService(serviceName);
        OutboundRouterCollection routerCollection =
            (OutboundRouterCollection) service.getOutboundMessageProcessor();
        return routerCollection.getRoutes().get(0);
    }

    protected MessageProcessor lookupInboundRouterFromService(String serviceName) throws Exception
    {
        Service service = lookupService(serviceName);
        List<MessageProcessor> routers =
            ((ServiceCompositeMessageSource) service.getMessageSource()).getMessageProcessors();
        assertEquals(1, routers.size());
        return routers.get(0);
    }

    protected Service lookupService(String serviceName)
    {
        Service service = muleContext.getRegistry().lookupService(serviceName);
        assertNotNull(service);
        return service;
    }
    
    public static class CustomOutboundRouter extends AbstractOutboundRouter
    {
        public boolean isMatch(MuleMessage message) throws MuleException
        {
            return true;
        }

        @Override
        protected MuleEvent route(MuleEvent event) throws MessagingException
        {
            return event;
        }
    }
}
