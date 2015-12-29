/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.mule.api.NameableObject;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.processor.chain.InterceptingChainLifecycleWrapper;
import org.mule.functional.junit4.FunctionalTestCase;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;

public class ResponseTransformerTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/config/response-transformer-test-flow.xml";
    }

    @Test
    public void testTransformers()
    {
        ImmutableEndpoint endpoint = muleContext.getRegistry().lookupObject("endpoint");
        assertFalse(endpoint.getMessageProcessors().isEmpty());
        assertEquals(2, endpoint.getMessageProcessors().size());
        checkNames("normal", endpoint.getMessageProcessors());
        assertFalse(endpoint.getResponseMessageProcessors().isEmpty());
        final List<MessageProcessor> messageProcessors = ((InterceptingChainLifecycleWrapper) endpoint.getResponseMessageProcessors().get(0)).getMessageProcessors();
        assertEquals(2, messageProcessors.size());
        checkNames("response", messageProcessors);
    }

    protected void checkNames(String prefix, List<MessageProcessor> transformers)
    {
        Iterator<MessageProcessor> iterator = transformers.iterator();
        for (int count = 1; iterator.hasNext(); count++)
        {
            NameableObject transformer = (NameableObject) iterator.next();
            logger.debug(transformer);
            assertEquals(prefix + count, transformer.getName());
        }
    }
}
