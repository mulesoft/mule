/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.simple;

import org.mule.api.transformer.Transformer;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transformer.simple.ValueExtractorTransformer;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ValueExtractorTransformerConfigTestCase extends FunctionalTestCase
{

    private List<ValueExtractorTransformer.ValueExtractorTemplate> expectedTemplates;
    private ValueExtractorTransformer.ValueExtractorTemplate template1;
    private ValueExtractorTransformer.ValueExtractorTemplate template2;
    private String expectedSource;

    @Override
    protected String getConfigResources()
    {
        return "value-extractor-transformer-config.xml";
    }

    @Before
    public void setUp()
    {
        expectedTemplates = new ArrayList<ValueExtractorTransformer.ValueExtractorTemplate>();

        template1 = new ValueExtractorTransformer.ValueExtractorTemplate();
        template1.setPattern("TEST(\\w+)TEST");
        template1.setTarget("#[header:INVOCATION:propName1]");
        template1.setFailIfNoMatch(false);
        template1.setDefaultValue("foo");

        template2 = new ValueExtractorTransformer.ValueExtractorTemplate();
        template2.setPattern("test(\\w+)test");
        template2.setTarget("#[header:OUTBOUND:propName2]");
        template2.setFailIfNoMatch(true);
    }

    @Test
    public void testConfiguresValueExtractor() throws Exception
    {
        expectedSource = "#[payload]";
        expectedTemplates.add(template1);
        expectedTemplates.add(template2);

        ValueExtractorTransformer valueExtractorTransformer = getValueExtractor("valueExtractor1");

        assertExpectedValueExtractor(valueExtractorTransformer);
    }

    @Test
    public void testDefaultExpression() throws Exception
    {
        expectedSource = "#[payload:]";
        expectedTemplates.add(template1);

        ValueExtractorTransformer valueExtractorTransformer = getValueExtractor("valueExtractor2");

        assertExpectedValueExtractor(valueExtractorTransformer);
    }

    private ValueExtractorTransformer getValueExtractor(String valueExtractor)
    {
        Transformer transformer = muleContext.getRegistry().lookupTransformer(valueExtractor);
        assertTrue(transformer instanceof ValueExtractorTransformer);
        return (ValueExtractorTransformer) transformer;
    }

    private void assertEqualTemplateList(List<ValueExtractorTransformer.ValueExtractorTemplate> templates, List<ValueExtractorTransformer.ValueExtractorTemplate> actualExtractorTemplates)
    {
        assertEquals(templates.size(), actualExtractorTemplates.size());

        int i = 0;
        for (ValueExtractorTransformer.ValueExtractorTemplate expectedTemplate: templates)
        {
            ValueExtractorTransformer.ValueExtractorTemplate actualTemplate =  actualExtractorTemplates.get(i++);

            assertEquals(expectedTemplate.getPattern(), actualTemplate.getPattern());
            assertEquals(expectedTemplate.getTarget(), actualTemplate.getTarget());
            assertEquals(expectedTemplate.isFailIfNoMatch(), actualTemplate.isFailIfNoMatch());
            assertEquals(expectedTemplate.getDefaultValue(), actualTemplate.getDefaultValue());
        }
    }

    private void assertExpectedValueExtractor(ValueExtractorTransformer valueExtractorTransformer)
    {
        assertEquals(expectedSource, valueExtractorTransformer.getSource());
        assertEqualTemplateList(expectedTemplates, valueExtractorTransformer.getValueExtractorTemplates());
    }

}
