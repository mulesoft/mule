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

import org.mule.api.MuleEventKeyGenerator;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;
import org.mule.cache.CachingMessageProcessor;
import org.mule.cache.CachingStrategy;
import org.mule.cache.ObjectStoreCachingStrategy;
import org.mule.keygenerator.ExpressionMuleEventKeyGenerator;
import org.mule.cache.responsegenerator.ResponseGenerator;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;

import java.io.NotSerializableException;
import java.io.Serializable;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CachingStrategyConfigTestCase extends FunctionalTestCase
{

    public CachingStrategyConfigTestCase()
    {
        setDisposeContextPerClass(true);
    }

    @Override
    protected String getConfigResources()
    {
        return "org/mule/cache/config/caching-strategy-config.xml";
    }

    @Test
    public void testKeyGenerationExpressionConfig() throws Exception
    {
        String cacheFlow = "CacheRouterWithkeyGenerationExpression";
        CachingMessageProcessor cacheMessageProcessor = getCachingMessageProcessorFromFlow(cacheFlow);

        CachingStrategy cachingStrategy = cacheMessageProcessor.getCachingStrategy();
        assertTrue(cachingStrategy instanceof ObjectStoreCachingStrategy);
        ObjectStoreCachingStrategy objectStoreCachingStrategy = (ObjectStoreCachingStrategy) cachingStrategy;

        assertTrue(objectStoreCachingStrategy.getKeyGenerator() instanceof ExpressionMuleEventKeyGenerator);
        ExpressionMuleEventKeyGenerator keyGenerator = (ExpressionMuleEventKeyGenerator) objectStoreCachingStrategy.getKeyGenerator();
        assertEquals("#[payload]", keyGenerator.getExpression());
    }

    @Test
    public void testKeyGeneratorConfig() throws Exception
    {
        String cacheFlow = "CacheRouterWithKeyGenerator";
        CachingMessageProcessor cacheMessageProcessor = getCachingMessageProcessorFromFlow(cacheFlow);

        CachingStrategy cachingStrategy = cacheMessageProcessor.getCachingStrategy();
        assertTrue(cachingStrategy instanceof ObjectStoreCachingStrategy);
        ObjectStoreCachingStrategy objectStoreCachingStrategy = (ObjectStoreCachingStrategy) cachingStrategy;

        assertTrue(objectStoreCachingStrategy.getKeyGenerator() instanceof TestMuleEventKeyGenerator);
    }

    @Test
    public void testResponseGeneratorConfig() throws Exception
    {
        String cacheFlow = "CacheRouterWithResponseGenerator";
        CachingMessageProcessor cacheMessageProcessor = getCachingMessageProcessorFromFlow(cacheFlow);

        CachingStrategy cachingStrategy = cacheMessageProcessor.getCachingStrategy();
        assertTrue(cachingStrategy instanceof ObjectStoreCachingStrategy);
        ObjectStoreCachingStrategy objectStoreCachingStrategy = (ObjectStoreCachingStrategy) cachingStrategy;

        assertTrue(objectStoreCachingStrategy.getResponseGenerator() instanceof TestResponseGenerator);
    }

    @Test
    public void testConsumableFilterConfig() throws Exception
    {
        String cacheFlow = "CacheRouterWithConsumableFilter";
        CachingMessageProcessor cacheMessageProcessor = getCachingMessageProcessorFromFlow(cacheFlow);

        CachingStrategy cachingStrategy = cacheMessageProcessor.getCachingStrategy();
        assertTrue(cachingStrategy instanceof ObjectStoreCachingStrategy);
        ObjectStoreCachingStrategy objectStoreCachingStrategy = (ObjectStoreCachingStrategy) cachingStrategy;

        assertTrue(objectStoreCachingStrategy.getConsumableFilter() instanceof TestConsumableFilter);
    }

    private CachingMessageProcessor getCachingMessageProcessorFromFlow(String cacheFlow)
    {
        Flow flow = (Flow) muleContext.getRegistry().get(cacheFlow);
        assertNotNull(flow);
        return (CachingMessageProcessor) flow.getMessageProcessors().get(0);
    }

    public static class TestMuleEventKeyGenerator implements MuleEventKeyGenerator
    {

        public TestMuleEventKeyGenerator()
        {

        }

        public Serializable generateKey(MuleEvent event) throws NotSerializableException
        {
            return "theKey";
        }
    }

    public static class TestResponseGenerator implements ResponseGenerator
    {

        public MuleEvent create(MuleEvent request, MuleEvent cachedResponse)
        {
            return cachedResponse;
        }
    }

    public static class TestConsumableFilter implements Filter
    {

        public boolean accept(MuleMessage muleMessage)
        {
            return false;
        }
    }
}
