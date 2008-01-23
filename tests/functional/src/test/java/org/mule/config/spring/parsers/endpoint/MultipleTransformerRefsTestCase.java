/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.endpoint;

import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transformer.Transformer;

import java.util.List;

/**
 * This really tests the handling of multiple references in
 * {@link org.mule.config.spring.parsers.assembly.DefaultBeanAssembler}
 */
public class MultipleTransformerRefsTestCase  extends AbstractEndpointTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/endpoint/multiple-transformer-refs-test.xml";
    }

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

    public void testSingleRef() throws MuleException
    {
        ImmutableEndpoint endpoint = doTest("single");
        List transformers = endpoint.getTransformers();
        assertNotNull(transformers);
        assertEquals(1, transformers.size());
        assertEquals("a", ((Transformer) transformers.get(0)).getName());
    }

}
