/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.config;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import org.junit.Test;
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
        assertThat(manager, is(not(nullValue())));
        assertThat(manager.isIncludeConfigNamespaces(), is(TRUE));
        assertThat(manager.getNamespaces().keySet(), hasSize(5));
    }

    @Test
    public void testJXPathFilterConfig() throws Exception
    {
        EndpointBuilder epb = muleContext.getRegistry().lookupEndpointBuilder("test.ep1");

        InboundEndpoint ep = epb.buildInboundEndpoint();
        assertThat(ep.getFilter(), is(not(nullValue())));
        assertThat(ep.getFilter(), instanceOf(JXPathFilter.class));
        JXPathFilter filter = (JXPathFilter) ep.getFilter();
        filter.initialise();
        assertThat(filter.getPattern(), equalTo("/bar:foo/bar:bar"));
        assertThat(filter.getNamespaces().keySet(), hasSize(6));
        assertThat(filter.getNamespaces().get("bar"), equalTo("http://bar.com"));
    }

    @Test
    public void testJaxenFilterConfig() throws Exception
    {
        EndpointBuilder epb = muleContext.getRegistry().lookupEndpointBuilder("test.ep2");

        InboundEndpoint ep = epb.buildInboundEndpoint();
        assertThat(ep.getFilter(), is(not(nullValue())));
        assertThat(ep.getFilter(), instanceOf(JaxenFilter.class));
        JaxenFilter filter = (JaxenFilter) ep.getFilter();
        assertThat(filter.getPattern(), equalTo("/car:foo/car:bar"));
        assertThat(filter.getNamespaces().keySet(), hasSize(6));
        assertThat(filter.getNamespaces().get("car"), equalTo("http://car.com"));
    }

    @Test
    public void testJXPathExtractor() throws Exception
    {
        JXPathExtractor transformer = (JXPathExtractor) lookupTransformer("jxpath-extractor");
        transformer.initialise();
        assertThat(transformer.getNamespaces(), is(not(nullValue())));
        assertThat(transformer.getNamespaces().keySet(), hasSize(6));
        assertThat(transformer.getNamespaces().get("foo"), is(not(nullValue())));
        assertThat(transformer.getNamespaces().get("bar"), is(not(nullValue())));
    }

    @Test
    public void testJaxbConfig() throws Exception
    {
        JAXBMarshallerTransformer t = (JAXBMarshallerTransformer) lookupTransformer("ObjectToXml");
        assertThat(t.getJaxbContext(), is(not(nullValue())));

        JAXBUnmarshallerTransformer t2 = (JAXBUnmarshallerTransformer) lookupTransformer("XmlToObject");
        assertThat((Class<Person>) t2.getReturnDataType().getType(), equalTo(Person.class));
        assertThat(t2.getJaxbContext(), is(not(nullValue())));
    }

    @Test
    public void testSchemaValidationFilterWithCustomResourceResolver()
    {
        SchemaValidationFilter filter = (SchemaValidationFilter) lookupFilter("SchemaValidationWithResourceResolver");
        assertThat(filter.getSchemaLocations(), equalTo("schema/schema1.xsd"));
        assertThat(filter.getResourceResolver(), instanceOf(MockResourceResolver.class));
        assertThat(filter.getErrorHandler(), instanceOf(MockErrorHandler.class));
        assertThat(filter.isReturnResult(), is(FALSE));
    }

    @Test
    public void testSchemaValidationFilterWithResourceResolverAndCustomSecurity()
    {
        SchemaValidationFilter filter = (SchemaValidationFilter) lookupFilter("SchemaValidationWithResourceAndCustomSecurityResolver");
        assertThat(filter.isAcceptExternalEntities(), equalTo(TRUE));
        assertThat(filter.isExpandInternalEntities(), equalTo(TRUE));
        assertThat(filter.getSchemaLocations(), equalTo("schema/schema1.xsd"));
        assertThat(filter.getResourceResolver(), instanceOf(MockResourceResolver.class));
        assertThat(filter.getErrorHandler(), instanceOf(MockErrorHandler.class));
        assertThat(filter.isReturnResult(), equalTo(FALSE));
    }

    private Transformer lookupTransformer(String name)
    {
        Transformer transformer = muleContext.getRegistry().lookupTransformer(name);
        assertThat(transformer, is(not(nullValue())));
        return transformer;
    }

    private Filter lookupFilter(String name)
    {
        Filter filter = muleContext.getRegistry().lookupObject(name);
        assertThat(filter, is(not(nullValue())));
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
