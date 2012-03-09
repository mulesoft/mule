/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.cache.config;

import org.mule.cache.CachingMessageProcessor;
import org.mule.cache.CachingStrategy;
import org.mule.cache.ObjectStoreCachingStrategy;
import org.mule.cache.filter.ConsumableMuleMessageFilter;
import org.mule.keygenerator.MD5MuleEventKeyGenerator;
import org.mule.cache.responsegenerator.DefaultResponseGenerator;
import org.mule.construct.Flow;
import org.mule.routing.filters.AcceptAllFilter;
import org.mule.routing.filters.ExpressionFilter;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.store.InMemoryObjectStore;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CacheConfigTestCase extends FunctionalTestCase
{

    public CacheConfigTestCase()
    {
        setDisposeContextPerClass(true);
    }

    @Override
    protected String getConfigResources()
    {
        return "org/mule/cache/config/cache-config.xml";
    }

    @Test
    public void testMessageProcessorDefaultConfig() throws Exception
    {
        String cacheFlow = "CacheRouterDefault";
        CachingMessageProcessor cacheMessageProcessor = getCachingMessageProcessorFromFlow(cacheFlow);
        assertTrue(cacheMessageProcessor.getFilter() instanceof AcceptAllFilter);

        assertDefaultCachingStrategy(cacheMessageProcessor.getCachingStrategy());
    }

    @Test
    public void testMessageProcessorFilterExpressionConfig() throws Exception
    {
        String cacheFlow = "CacheRouterWithFilterExpression";
        CachingMessageProcessor cacheMessageProcessor = getCachingMessageProcessorFromFlow(cacheFlow);
        assertTrue(cacheMessageProcessor.getFilter() instanceof ExpressionFilter);

        ExpressionFilter cacheFilter = (ExpressionFilter) cacheMessageProcessor.getFilter();
        assertEquals("//isCacheable[text() = 'true']", cacheFilter.getExpression());
        assertEquals("xpath", cacheFilter.getEvaluator());

        assertDefaultCachingStrategy(cacheMessageProcessor.getCachingStrategy());
    }

    @Test
    public void testMessageProcessorFilterConfig() throws Exception
    {
        String cacheFlow = "CacheRouterWithFilter";
        CachingMessageProcessor cacheMessageProcessor = getCachingMessageProcessorFromFlow(cacheFlow);
        assertTrue(cacheMessageProcessor.getFilter() instanceof ExpressionFilter);

        assertDefaultCachingStrategy(cacheMessageProcessor.getCachingStrategy());
    }

    @Test
    public void testMessageProcessorCachingStrategyConfig() throws Exception
    {
        String cacheFlow = "CacheRouterWithSpringObjectStore";
        CachingMessageProcessor cacheMessageProcessor = getCachingMessageProcessorFromFlow(cacheFlow);
        assertTrue(cacheMessageProcessor.getFilter() instanceof AcceptAllFilter);

        CachingStrategy cachingStrategy = cacheMessageProcessor.getCachingStrategy();

        assertTrue(cachingStrategy instanceof ObjectStoreCachingStrategy);
        ObjectStoreCachingStrategy objectStoreCachingStrategy = (ObjectStoreCachingStrategy) cachingStrategy;

        assertTrue(objectStoreCachingStrategy.getConsumableFilter() instanceof ConsumableMuleMessageFilter);
        assertTrue(objectStoreCachingStrategy.getKeyGenerator() instanceof MD5MuleEventKeyGenerator);
        assertEquals("org.mule.util.store.TextFileObjectStore",  objectStoreCachingStrategy.getStore().getClass().getName());
        assertTrue(objectStoreCachingStrategy.getResponseGenerator() instanceof DefaultResponseGenerator);
    }

    private void assertDefaultCachingStrategy(CachingStrategy cachingStrategy)
    {
        assertTrue(cachingStrategy instanceof ObjectStoreCachingStrategy);
        ObjectStoreCachingStrategy objectStoreCachingStrategy = (ObjectStoreCachingStrategy) cachingStrategy;

        assertTrue(objectStoreCachingStrategy.getConsumableFilter() instanceof ConsumableMuleMessageFilter);
        assertTrue(objectStoreCachingStrategy.getKeyGenerator() instanceof MD5MuleEventKeyGenerator);
        assertTrue(objectStoreCachingStrategy.getStore() instanceof InMemoryObjectStore);
        assertTrue(objectStoreCachingStrategy.getResponseGenerator() instanceof DefaultResponseGenerator);
    }

    private CachingMessageProcessor getCachingMessageProcessorFromFlow(String cacheFlow)
    {
        Flow flow = (Flow) muleContext.getRegistry().get(cacheFlow);
        assertNotNull(flow);
        return (CachingMessageProcessor) flow.getMessageProcessors().get(0);
    }
}
