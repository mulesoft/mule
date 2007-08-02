/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.modules.xml.config;

import org.mule.tck.FunctionalTestCase;
import org.mule.transformers.AbstractTransformer;
import org.mule.transformers.xml.DomDocumentToXml;
import org.mule.transformers.xml.JXPathExtractor;
import org.mule.transformers.xml.ObjectToXml;
import org.mule.transformers.xml.XmlToDomDocument;
import org.mule.transformers.xml.XmlToObject;
import org.mule.transformers.xml.XsltTransformer;
import org.mule.transformers.xml.AbstractXmlTransformer;
import org.mule.umo.transformer.UMOTransformer;

import javax.xml.transform.TransformerFactory;

public class XmlTransformerNamespaceHandlerTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "xml/xml-transformer-namespace-test.xml";
    }

    public void testDomToXml()
    {
        getAndTestTransformer("domToXml", DomDocumentToXml.class);
    }

    public void testJXPathExtractor()
    {
        JXPathExtractor extractor =
                (JXPathExtractor) getAndTestTransformer("jxpathExtractor", JXPathExtractor.class);
        assertEquals("/expression", extractor.getExpression());
        assertFalse(extractor.isSingleResult());
    }

    public void testObjectToXml()
    {
        ObjectToXml objectToXml = (ObjectToXml) getAndTestTransformer("objectToXml", ObjectToXml.class);
        assertTrue(objectToXml.isAcceptUMOMessage());
    }

    public void testXmlToDom()
    {
        getAndTestTransformer("xmlToDom", XmlToDomDocument.class);
    }

    public void testXmlToObject()
    {
        getAndTestTransformer("xmlToObject", XmlToObject.class);
    }

    public void testXslt()
    {
        XsltTransformer xslt = (XsltTransformer) getAndTestTransformer("xslt", XsltTransformer.class);
        assertEquals(10, xslt.getMaxActiveTransformers());
        assertEquals(10, xslt.getMaxIdleTransformers());
        assertEquals("org.apache.xalan.processor.TransformerFactoryImpl", xslt.getXslTransformerFactory());
        assertEquals("file", xslt.getXslFile());
        assertNotNull(xslt.getXslt());
        String transform = xslt.getXslt();
        assertTrue(transform.indexOf("test for this string in test") > -1);
    }

    protected AbstractTransformer getAndTestTransformer(String name, Class clazz)
    {
        assertTrue(AbstractTransformer.class.isAssignableFrom(clazz));
        UMOTransformer object= managementContext.getRegistry().lookupTransformer(name);
        assertNotNull(object);
        assertTrue(clazz.isAssignableFrom(object.getClass()));
        AbstractTransformer transformer = (AbstractTransformer) object;
        assertAbstractTransformerOk(transformer, name);
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