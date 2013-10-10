/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
