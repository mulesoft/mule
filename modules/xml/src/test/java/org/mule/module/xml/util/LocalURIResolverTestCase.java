
package org.mule.module.xml.util;

import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

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
