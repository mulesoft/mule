/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.api.processor.MessageProcessor;
import org.mule.construct.Flow;
import org.mule.routing.IdempotentMessageFilter;
import org.mule.routing.IdempotentSecureHashMessageFilter;
import org.mule.routing.MessageFilter;
import org.mule.routing.filters.WildcardFilter;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transformer.simple.CombineCollectionsTransformer;

import java.util.List;

import org.junit.Test;

public class GlobalInterceptingMessageProcessorsTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "global-intercepting-mps-config.xml";
    }

    @Test
    public void testConfig() throws Exception
    {
        Flow flow1 = muleContext.getRegistry().lookupObject("flow1");
        assertNotNull(flow1);
        List<MessageProcessor> mpList = flow1.getMessageProcessors();

        MessageProcessor mp1 = muleContext.getRegistry().lookupObject("idempotentFilter");
        assertTrue(mp1 instanceof IdempotentMessageFilter);
        IdempotentMessageFilter imf = (IdempotentMessageFilter) mp1;
        assertEquals(imf.getIdExpression(), "#[payload:]");
        assertMpPresent(mpList, mp1, IdempotentMessageFilter.class);

        MessageProcessor mp2 = muleContext.getRegistry().lookupObject("messageFilter");
        assertTrue(mp2 instanceof MessageFilter);
        MessageFilter mf = (MessageFilter) mp2;
        assertTrue(mf.getFilter() instanceof WildcardFilter);
        assertFalse(mf.isThrowOnUnaccepted());
        assertMpPresent(mpList, mp2, MessageFilter.class);

        MessageProcessor mp3 = muleContext.getRegistry().lookupObject("idempotentSecureHashMessageFilter");
        assertTrue(mp3 instanceof IdempotentSecureHashMessageFilter);
        IdempotentSecureHashMessageFilter ishmf = (IdempotentSecureHashMessageFilter) mp3;
        assertEquals(ishmf.getMessageDigestAlgorithm(), "MDA5");
        assertMpPresent(mpList, mp3, IdempotentSecureHashMessageFilter.class);

        MessageProcessor mp4 = muleContext.getRegistry().lookupObject("combineCollectionsTransformer");
        assertTrue(mp4 instanceof CombineCollectionsTransformer);
        assertMpPresent(mpList, mp4, CombineCollectionsTransformer.class);
    }

    /**
     * Check that the list of message processors contains a duplicate of the MP looked up
     * in the registry (ie. that the MP is a prototype, not a singleton)
     */
    private void assertMpPresent(List<MessageProcessor> mpList, MessageProcessor mp, Class<?> clazz)
    {
        assertFalse(mpList.contains(mp));

        for (MessageProcessor theMp : mpList)
        {
            if (clazz.isInstance(theMp))
            {
                return;
            }
        }

        fail("No " + clazz.getSimpleName() + " found");
    }
}
