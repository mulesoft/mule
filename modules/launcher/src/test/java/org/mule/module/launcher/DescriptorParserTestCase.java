/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import org.mule.config.Preferred;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.module.launcher.descriptor.DescriptorParser;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.File;
import java.io.IOException;

import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@SmallTest
public class DescriptorParserTestCase extends AbstractMuleTestCase
{

    @Test
    public void testOverridePreferred() throws Exception
    {
        DefaultAppBloodhound bh = new DefaultAppBloodhound();
        MultiMap overrides = new MultiValueMap();
        overrides.put("properties", new TestDescriptorParserDefault());

        // test with default annotation values
        bh.mergeParserOverrides(overrides);
        assertEquals(1, bh.parserRegistry.size());
        DescriptorParser result = bh.parserRegistry.get("properties");
        assertNotNull(result);
        assertTrue("@Preferred implementation ignored", result instanceof TestDescriptorParserDefault);
    }

    @Test
    public void testBothPreferredWithWeight() throws Exception
    {
        DefaultAppBloodhound bh = new DefaultAppBloodhound();
        MultiMap overrides = new MultiValueMap();
        overrides.put("properties", new TestDescriptorParserDefault());
        overrides.put("properties", new TestDescriptorParserPreferred());

        // test with weigh attribute (we have 3 candidates now)
        bh.mergeParserOverrides(overrides);
        assertEquals(1, bh.parserRegistry.size());
        DescriptorParser result = bh.parserRegistry.get("properties");
        assertNotNull(result);
        assertTrue("@Preferred implementation ignored", result instanceof TestDescriptorParserPreferred);
    }

    @Test
    public void testOverrideWithoutPreferred() throws Exception
    {
        DefaultAppBloodhound bh = new DefaultAppBloodhound();
        MultiMap overrides = new MultiValueMap();
        overrides.put("properties", new TestDescriptorParserNoAnnotation());

        // test with weigh attribute (we have 3 candidates now)
        bh.mergeParserOverrides(overrides);
        assertEquals(1, bh.parserRegistry.size());
        DescriptorParser result = bh.parserRegistry.get("properties");
        assertNotNull(result);
        assertTrue("@Preferred implementation ignored", result instanceof TestDescriptorParserNoAnnotation);
    }

    @Test
    public void testMixedOverrides() throws Exception
    {
        DefaultAppBloodhound bh = new DefaultAppBloodhound();
        MultiMap overrides = new MultiValueMap();
        overrides.put("properties", new TestDescriptorParserNoAnnotation());
        overrides.put("properties", new TestDescriptorParserDefault());

        // test with weigh attribute (we have 3 candidates now)
        bh.mergeParserOverrides(overrides);
        assertEquals(1, bh.parserRegistry.size());
        DescriptorParser result = bh.parserRegistry.get("properties");
        assertNotNull(result);
        assertTrue("@Preferred implementation ignored", result instanceof TestDescriptorParserDefault);
    }


    /**
     * Test parser with annotation default
     */
    @Preferred()
    class TestDescriptorParserDefault implements DescriptorParser
    {

        @Override
        public ApplicationDescriptor parse(File descriptor, String applicationName) throws IOException
        {
            return null;
        }

        public String getSupportedFormat()
        {
            return "properties";
        }
    }

    /**
     * Test parser with weigh annotation
     */
    @Preferred(weight = 10)
    class TestDescriptorParserPreferred implements DescriptorParser
    {

        @Override
        public ApplicationDescriptor parse(File descriptor, String applicationName) throws IOException
        {
            return null;
        }

        public String getSupportedFormat()
        {
            return "properties";
        }

    }


    class TestDescriptorParserNoAnnotation implements DescriptorParser
    {

        @Override
        public ApplicationDescriptor parse(File descriptor, String applicationName) throws IOException
        {
            return null;
        }

        public String getSupportedFormat()
        {
            return "properties";
        }

    }

}
