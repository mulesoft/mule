/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.endpoint;

import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transformer.Transformer;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * This really tests the handling of multiple references in
 * {@link org.mule.config.spring.parsers.assembly.DefaultBeanAssembler}
 */
public class MultipleTransformerRefsTestCase  extends AbstractEndpointTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/endpoint/multiple-transformer-refs-test.xml";
    }

    @Test
    public void testMultipleRefs() throws MuleException
    {
        ImmutableEndpoint endpoint = doTest("many");
        List transformers = endpoint.getTransformers();
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
        List transformers = endpoint.getTransformers();
        assertNotNull(transformers);
        assertEquals(1, transformers.size());
        assertEquals("a", ((Transformer) transformers.get(0)).getName());
    }

}
