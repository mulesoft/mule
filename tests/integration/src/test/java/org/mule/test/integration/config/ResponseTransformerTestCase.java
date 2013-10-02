/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.config;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transformer.Transformer;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ResponseTransformerTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/config/response-transformer-test-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/config/response-transformer-test-flow.xml"}});
    }

    public ResponseTransformerTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testTransformers()
    {
        ImmutableEndpoint endpoint = (ImmutableEndpoint) muleContext.getRegistry().lookupObject("endpoint");
        assertFalse(endpoint.getTransformers().isEmpty());
        assertEquals(2, endpoint.getTransformers().size());
        checkNames("normal", endpoint.getTransformers());
        assertFalse(endpoint.getResponseTransformers().isEmpty());
        assertEquals(2, endpoint.getResponseTransformers().size());
        checkNames("response", endpoint.getResponseTransformers());
    }

    protected void checkNames(String prefix, List<Transformer> transformers)
    {
        Iterator<Transformer> iterator = transformers.iterator();
        for (int count = 1; iterator.hasNext(); count++)
        {
            Transformer transformer = iterator.next();
            logger.debug(transformer);
            assertEquals(prefix + count, transformer.getName());
        }
    }
}
