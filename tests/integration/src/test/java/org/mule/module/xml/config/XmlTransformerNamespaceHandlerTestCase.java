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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleException;
import org.mule.api.transformer.Transformer;
import org.mule.module.xml.transformer.AbstractXmlTransformer;
import org.mule.module.xml.transformer.DomDocumentToXml;
import org.mule.module.xml.transformer.JXPathExtractor;
import org.mule.module.xml.transformer.ObjectToXml;
import org.mule.module.xml.transformer.XmlToDomDocument;
import org.mule.module.xml.transformer.XmlToObject;
import org.mule.module.xml.transformer.XsltTransformer;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transformer.AbstractTransformer;

import java.util.Map;

import org.junit.Test;

public class XmlTransformerNamespaceHandlerTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/module/xml/xml-transformer-namespace-test.xml";
    }

    @Test
    public void testDomToXml()
    {
        getAndTestTransformer("domToXml", DomDocumentToXml.class);
    }

    @Test
    public void testJXPathExtractor()
    {
        JXPathExtractor extractor = (JXPathExtractor) getAndTestTransformer("jxpathExtractor",
            JXPathExtractor.class);
        assertEquals("/expression", extractor.getExpression());
        assertFalse(extractor.isSingleResult());
        assertEquals(JXPathExtractor.OUTPUT_TYPE_VALUE, extractor.getOutputType());
        
        Map ns = extractor.getNamespaces();
        assertNotNull(ns);
        
        assertEquals("http://foo.com", ns.get("foo1"));
        assertEquals("http://foo.com", ns.get("foo2"));
    }

    @Test
    public void testObjectToXml()
    {
        ObjectToXml objectToXml = (ObjectToXml) getAndTestTransformer("objectToXml", ObjectToXml.class);
        assertTrue(objectToXml.isAcceptMuleMessage());
    }

    @Test
    public void testXmlToDom()
    {
        getAndTestTransformer("xmlToDom", XmlToDomDocument.class);
    }

    @Test
    public void testXmlToObject()
    {
        getAndTestTransformer("xmlToObject", XmlToObject.class);
    }

    @Test
    public void testXslt()
    {
        XsltTransformer xslt = (XsltTransformer) getAndTestTransformer("xslt", XsltTransformer.class);
        assertEquals(10, xslt.getMaxActiveTransformers());
        assertEquals(10, xslt.getMaxIdleTransformers());
        assertEquals(CustomXsltTransformerFactory.class.getName(), xslt.getXslTransformerFactory());
        assertNull(xslt.getXslFile());
        assertNotNull(xslt.getXslt());
        String transform = xslt.getXslt();
        assertTrue(transform.indexOf("test for this string in test") > -1);

        assertEquals("#[header:foo]", xslt.getContextProperties().get("bar"));
    }

    @Test
    public void testDomToXmlOnEndpoint() throws MuleException
    {
        getAndTestEndpointTransformer("ep1", DomDocumentToXml.class);
    }

    @Test
    public void testJXPathExtractorOnEndpoint() throws MuleException
    {
        JXPathExtractor extractor = (JXPathExtractor) getAndTestEndpointTransformer("ep2",
            JXPathExtractor.class);
        assertEquals("/expression", extractor.getExpression());
        assertFalse(extractor.isSingleResult());
    }

    @Test
    public void testObjectToXmlOnEndpoint() throws MuleException
    {
        ObjectToXml objectToXml = (ObjectToXml) getAndTestEndpointTransformer("ep3", ObjectToXml.class);
        assertTrue(objectToXml.isAcceptMuleMessage());
    }

    @Test
    public void testXmlToDomOnEndpoint() throws MuleException
    {
        getAndTestEndpointTransformer("ep4", XmlToDomDocument.class);
    }

    @Test
    public void testXmlToObjectOnEndpoint() throws MuleException
    {
        getAndTestEndpointTransformer("ep5", XmlToObject.class);
    }

    @Test
    public void testXsltOnEndpoint() throws MuleException
    {
        XsltTransformer xslt = (XsltTransformer) getAndTestEndpointTransformer("ep6", XsltTransformer.class);
        assertEquals(10, xslt.getMaxActiveTransformers());
        assertEquals(10, xslt.getMaxIdleTransformers());
        assertEquals(CustomXsltTransformerFactory.class.getName(), xslt.getXslTransformerFactory());
        assertNotNull(xslt.getXslt());
        String transform = xslt.getXslt();
        assertTrue(transform.indexOf("test for this string in test") > -1);

        assertEquals("#[header:foo]", xslt.getContextProperties().get("bar"));
    }

    protected AbstractTransformer getAndTestTransformer(String name, Class clazz)
    {
        assertTrue(AbstractTransformer.class.isAssignableFrom(clazz));
        Transformer object= muleContext.getRegistry().lookupTransformer(name);

        assertNotNull(object);
        assertTrue(clazz.isAssignableFrom(object.getClass()));
        AbstractTransformer transformer = (AbstractTransformer) object;
        assertAbstractTransformerOk(transformer, name);
        return transformer;
    }
    
    protected AbstractTransformer getAndTestEndpointTransformer(String endpointName, Class clazz) throws MuleException
    {
        assertTrue(AbstractTransformer.class.isAssignableFrom(clazz));
        assertEquals(1, muleContext.getEndpointFactory().getInboundEndpoint(endpointName).getTransformers().size());
        AbstractTransformer transformer= (AbstractTransformer) muleContext.getEndpointFactory().getInboundEndpoint(endpointName).getTransformers().get(0);

        assertNotNull(transformer);
        assertTrue(clazz.isAssignableFrom(transformer.getClass()));
        return transformer;
    }

    protected void assertAbstractTransformerOk(AbstractTransformer transformer, String name)
    {
        assertTrue(transformer.isIgnoreBadInput());
        assertEquals(Object.class, transformer.getReturnClass());
        assertEquals(name, transformer.getName());
        // AbstractXmlTransformer instances have an output encoding
        if (transformer instanceof AbstractXmlTransformer)
        {
            assertEquals("foo", ((AbstractXmlTransformer) transformer).getOutputEncoding());
        }
    }

}
