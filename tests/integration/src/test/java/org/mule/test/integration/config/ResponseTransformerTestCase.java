/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.config;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transformer.Transformer;
import org.mule.tck.FunctionalTestCase;
import org.mule.transformer.TransformerUtils;

import java.util.Iterator;
import java.util.List;

public class ResponseTransformerTestCase extends FunctionalTestCase
{

    public void testTransformers()
    {
        ImmutableEndpoint endpoint = (ImmutableEndpoint) muleContext.getRegistry().lookupObject("endpoint");
        assertTrue(TransformerUtils.isDefined(endpoint.getTransformers()));
        assertEquals(2, endpoint.getTransformers().size());
        checkNames("normal", endpoint.getTransformers());
        assertTrue(TransformerUtils.isDefined(endpoint.getResponseTransformers()));
        assertEquals(2, endpoint.getResponseTransformers().size());
        checkNames("response", endpoint.getResponseTransformers());
    }

    protected void checkNames(String prefix, List transformers)
    {
        Iterator iterator = transformers.iterator();
        for (int count = 1; iterator.hasNext(); count++)
        {
            Transformer transformer = (Transformer) iterator.next();
            logger.debug(transformer);
            assertEquals(prefix + count, transformer.getName());
        }
    }

    protected String getConfigResources()
    {
        return "org/mule/test/integration/config/response-transformer-test.xml";
    }

}
