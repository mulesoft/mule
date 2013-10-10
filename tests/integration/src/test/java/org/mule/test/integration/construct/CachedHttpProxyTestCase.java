/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
