/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import org.mule.tck.junit4.AbstractMuleTestCase;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerFactory;

import org.junit.Test;

/**
 * These tests require the system property as configured in surefire in this module.
 * <p>
 * The asserted classes were obtained from getting the factories in a java component in a standalone mule.
 */
public class XmlLibrariesVerificationTestCase extends AbstractMuleTestCase
{

    @Test
    public void documentBuilder()
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        assertThat(factory, instanceOf(org.apache.xerces.jaxp.DocumentBuilderFactoryImpl.class));
    }

    @Test
    public void saxParser()
    {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        assertThat(factory, instanceOf(org.apache.xerces.jaxp.SAXParserFactoryImpl.class));
    }
    
    @Test
    public void xmlInput()
    {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        assertThat(factory, instanceOf(com.ctc.wstx.stax.WstxInputFactory.class));
    }
    
    /**
     * Saxon is used explicitly.
     * <p>
     * When using TransformerFactory, a Xalan transformer must be returned since Xalan is in lib/endorsed in standalone. 
     */
    @Test
    public void transformer()
    {
        TransformerFactory factory = TransformerFactory.newInstance();
        assertThat(factory, instanceOf(org.apache.xalan.processor.TransformerFactoryImpl.class));
    }
}
