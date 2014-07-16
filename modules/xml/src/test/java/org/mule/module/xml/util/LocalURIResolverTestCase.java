/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.util;

import org.mule.tck.size.SmallTest;

import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

@SmallTest
public class LocalURIResolverTestCase
{
    @Test
    public void testResolveFullClasspath() throws TransformerException
    {
        // Original functionality
        LocalURIResolver resolver = new LocalURIResolver();
        StreamSource source = (StreamSource)resolver.resolve("xsl/include.xsl", null);
        assertNotNull("No input stream", source.getInputStream());
    }

    @Test
    public void testResolveRelativeClasspathToMainXslFile() throws TransformerException
    {
        // XSLT transformer configured with resource "xsl/test.xsl"
        LocalURIResolver resolver = new LocalURIResolver("xsl/include.xsl");

        // Relative resource path
        StreamSource source = (StreamSource)resolver.resolve("include.xsl", null);
        assertNotNull("No input stream", source.getInputStream());
    }

    @Test
    public void testResolveAboveInClasspath() throws TransformerException
    {
        // XSLT transformer configured with resource "xsl/test.xsl"
        LocalURIResolver resolver = new LocalURIResolver("xsl/include.xsl");

        // Relative resource path
        StreamSource source = (StreamSource)resolver.resolve("../test.xsl", null);
        assertNotNull("No input stream", source.getInputStream());
    }
}
