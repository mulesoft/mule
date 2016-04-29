/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.parsers.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.endpoint.ImmutableEndpoint;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.transformer.Transformer;

import java.util.List;

import org.junit.Test;

/**
 * This really tests the handling of multiple references in
 * {@link org.mule.config.spring.parsers.assembly.DefaultBeanAssembler}
 */
public class MultipleTransformerRefsTestCase  extends AbstractEndpointTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/parsers/endpoint/multiple-transformer-refs-test.xml";
    }

    @Test
    public void testMultipleRefs() throws MuleException
    {
        ImmutableEndpoint endpoint = doTest("many");
        List<MessageProcessor> transformers = endpoint.getMessageProcessors();
        assertNotNull(transformers);
        // this lets us check ordering before size, safely, which is useful on failure
        assertTrue(transformers.size() > 0);
        assertEquals("a", ((Transformer) transformers.get(0)).getName());
        assertTrue(transformers.size() > 1);
        assertEquals("b", ((Transformer) transformers.get(1)).getName());
        assertEquals(3, transformers.size());
        assertEquals("c", ((Transformer) transformers.get(2)).getName());
    }

    @Test
    public void testSingleRef() throws MuleException
    {
        ImmutableEndpoint endpoint = doTest("single");
        List<MessageProcessor> transformers = endpoint.getMessageProcessors();
        assertNotNull(transformers);
        assertEquals(1, transformers.size());
        assertEquals("a", ((Transformer) transformers.get(0)).getName());
    }

}
