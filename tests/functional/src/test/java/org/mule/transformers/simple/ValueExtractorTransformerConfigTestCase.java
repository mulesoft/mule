/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.simple;

import org.mule.api.transformer.Transformer;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transformer.simple.ValueExtractorTransformer;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ValueExtractorTransformerConfigTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "value-extractor-transformer-config.xml";
    }

    @Test
    public void testConfiguresValueExtractor() throws Exception
    {
        Transformer transformer = muleContext.getRegistry().lookupTransformer("valueExtractor");

        assertTrue(transformer instanceof ValueExtractorTransformer);
        ValueExtractorTransformer valueExtractorTransformer = (ValueExtractorTransformer) transformer;

        assertEquals("#[payload]", valueExtractorTransformer.getSource());
        List<ValueExtractorTransformer.ValueExtractorTemplate> valueExtractorTemplates = valueExtractorTransformer.getValueExtractorTemplates();
        assertEquals(2, valueExtractorTemplates.size());

        ValueExtractorTransformer.ValueExtractorTemplate valueExtractorTemplate = valueExtractorTemplates.get(0);
        assertEquals("TEST(\\w+)TEST", valueExtractorTemplate.getPattern());
        assertEquals("#[header:INVOCATION:propName1]", valueExtractorTemplate.getTarget());
        assertFalse(valueExtractorTemplate.isFailIfNoMatch());

        valueExtractorTemplate = valueExtractorTemplates.get(1);
        assertEquals("test(\\w+)test", valueExtractorTemplate.getPattern());
        assertEquals("#[header:OUTBOUND:propName2]", valueExtractorTemplate.getTarget());
        assertTrue(valueExtractorTemplate.isFailIfNoMatch());
    }
}
