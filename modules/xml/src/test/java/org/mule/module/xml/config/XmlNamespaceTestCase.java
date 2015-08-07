/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.routing.filter.Filter;
import org.mule.api.transformer.Transformer;
import org.mule.jaxb.model.Person;
import org.mule.module.xml.filters.JXPathFilter;
import org.mule.module.xml.filters.JaxenFilter;
import org.mule.module.xml.filters.SchemaValidationFilter;
import org.mule.module.xml.transformer.JXPathExtractor;
import org.mule.module.xml.transformer.jaxb.JAXBMarshallerTransformer;
import org.mule.module.xml.transformer.jaxb.JAXBUnmarshallerTransformer;
import org.mule.module.xml.util.NamespaceManager;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XmlNamespaceTestCase extends FunctionalTestCase
{
    public XmlNamespaceTestCase()
    {
        setDisposeContextPerClass(true);
    }

    @Override
    protected String getConfigFile()
    {
        return "xml-namespace-config.xml";
    }

    @Test
    public void testGlobalNamespaces() throws Exception
    {
        NamespaceManager manager = muleContext.getRegistry().lookupObject(NamespaceManager.class);
        assertNotNull(manager);
        assertTrue(manager.isIncludeConfigNamespaces());
        assertEquals(5, manager.getNamespaces().size());
    }

    @Test
    public void testJXPathFilterConfig() throws Exception
    {
        EndpointBuilder epb = muleContext.getRegistry().lookupEndpointBuilder("test.ep1");

        InboundEndpoint ep = epb.buildInboundEndpoint();
        assertNotNull(ep.getFilter());
        assertTrue(ep.getFilter() instanceof JXPathFilter);
        JXPathFilter filter = (JXPathFilter)ep.getFilter();
        filter.initialise();
        assertEquals("/bar:foo/bar:bar", filter.getPattern());
        assertEquals(6, filter.getNamespaces().size());
        assertEquals("http://bar.com", filter.getNamespaces().get("bar"));
    }

    @Test
    public void testJaxenFilterConfig() throws Exception
    {
        EndpointBuilder epb = muleContext.getRegistry().lookupEndpointBuilder("test.ep2");

        InboundEndpoint ep = epb.buildInboundEndpoint();
        assertNotNull(ep.getFilter());
        assertTrue(ep.getFilter() instanceof JaxenFilter);
        JaxenFilter filter = (JaxenFilter)ep.getFilter();
        assertEquals("/car:foo/car:bar", filter.getPattern());
        assertEquals(6, filter.getNamespaces().size());
        assertEquals("http://car.com", filter.getNamespaces().get("car"));
    }

    @Test
    public void testJXPathExtractor() throws Exception
    {
        JXPathExtractor transformer = (JXPathExtractor) lookupTransformer("jxpath-extractor");
        transformer.initialise();
        assertNotNull(transformer.getNamespaces());
        assertEquals(6, transformer.getNamespaces().size());
        assertNotNull(transformer.getNamespaces().get("foo"));
        assertNotNull(transformer.getNamespaces().get("bar"));
    }

    @Test
    public void testJaxbConfig() throws Exception
    {
        JAXBMarshallerTransformer t = (JAXBMarshallerTransformer) lookupTransformer("ObjectToXml");
        assertNotNull(t.getJaxbContext());

        JAXBUnmarshallerTransformer t2 = (JAXBUnmarshallerTransformer) lookupTransformer("XmlToObject");
        assertEquals(Person.class, t2.getReturnDataType().getType());
        assertNotNull(t2.getJaxbContext());
    }
    
    @Test
    public void testSchemaValidationFilterWithCustomResourceResolver()
    {
        SchemaValidationFilter filter = (SchemaValidationFilter) lookupFilter("SchemaValidationWithResourceResolver");
        assertEquals("schema1.xsd", filter.getSchemaLocations());
        assertTrue(filter.getResourceResolver() instanceof MockResourceResolver);
        assertTrue(filter.getErrorHandler() instanceof MockErrorHandler);
        assertFalse(filter.isReturnResult());
    }
    
    private Transformer lookupTransformer(String name)
    {
        Transformer transformer = muleContext.getRegistry().lookupTransformer(name);
        assertNotNull(transformer);
        return transformer;
    }
    
    private Filter lookupFilter(String name)
    {
        Filter filter = muleContext.getRegistry().lookupObject(name);
        assertNotNull(filter);
        return filter;
    }
    
    private static class MockResourceResolver implements LSResourceResolver
    {
        public LSInput resolveResource(String type, String namespaceURI, String publicId,
            String systemId, String baseURI)
        {
            return null;
        }
    }
    
    private static class MockErrorHandler implements ErrorHandler
    {
        public void error(SAXParseException exception) throws SAXException
        {
            // does nothing
        }

        public void fatalError(SAXParseException exception) throws SAXException
        {
            // does nothing
        }

        public void warning(SAXParseException exception) throws SAXException
        {
            // does nothing
        }
    }
}
