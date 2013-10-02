/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.construct;

import static org.junit.Assert.assertTrue;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.cache.CachingStrategy;
import org.mule.api.processor.MessageProcessor;

import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.runners.Parameterized;

/**
 * Tests that a HTTP-proxy work as intended when a caching strategy is
 * configured in order to check that the pattern is properly built.
 */
public class CachedHttpProxyTestCase extends HttpProxyTestCase
{
    private static boolean invokedCache;

    public CachedHttpProxyTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {{ConfigVariant.SERVICE,
                "org/mule/test/integration/construct/cached-http-proxy-config.xml"}
        });
    }

    @Before
    public void setUp() throws Exception
    {
        invokedCache = false;
    }

    @After
    public void tearDown() throws Exception
    {
        assertTrue("Cache was never invoked", invokedCache);
    }

    public static class TestCachingStrategy implements CachingStrategy
    {

        @Override
        public MuleEvent process(MuleEvent request, MessageProcessor messageProcessor) throws MuleException
        {
            invokedCache = true;
            return messageProcessor.process(request);
        }
    }
}
