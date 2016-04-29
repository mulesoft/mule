/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.runtime.module.xml.transformer.AbstractXmlTransformer;
import org.mule.runtime.module.xml.transformer.DomDocumentToXml;
import org.mule.runtime.module.xml.transformer.ObjectToXml;
import org.mule.runtime.module.xml.transformer.XmlToDomDocument;
import org.mule.runtime.module.xml.transformer.XmlToObject;
import org.mule.runtime.module.xml.transformer.XsltTransformer;

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
    
    protected void assertAbstractTransformerOk(AbstractTransformer transformer, String name)
    {
        assertTrue(transformer.isIgnoreBadInput());
        assertEquals(Object.class, transformer.getReturnDataType().getType());
        assertEquals(name, transformer.getName());
        // AbstractXmlTransformer instances have an output encoding
        if (transformer instanceof AbstractXmlTransformer)
        {
            assertEquals("foo", ((AbstractXmlTransformer) transformer).getOutputEncoding());
        }
    }

}
