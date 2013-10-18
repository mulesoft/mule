/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.stax;

import org.mule.tck.junit4.AbstractMuleTestCase;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ReversibleXMLStreamReaderTest extends AbstractMuleTestCase
{

    private JaxpStreamReaderAsserter asserter = null;
    
    @Before
    public void setUp() throws Exception
    {
        String factoryName = XMLInputFactory.newInstance().getClass().getName();
        if (factoryName.equals("com.sun.xml.internal.stream.XMLInputFactoryImpl"))
        {
            asserter = new JaxpStreamReaderAsserter();
        }
        else if (factoryName.equals("com.ctc.wstx.stax.WstxInputFactory"))
        {
            asserter = new StaxStreamReaderAsserter();
        }
        else
        {
            fail("Don't know how to handle XMLInputFactory \"" + factoryName + "\"");
        }
    }

    @Test
    public void testReverse() throws Exception
    {        
        ReversibleXMLStreamReader xsr = createReader();

        asserter.assertDocumentStart(xsr);
        QName start = xsr.getName();
        assertEquals(XMLStreamConstants.CHARACTERS, xsr.next());
        String text = xsr.getText();

        xsr.reset();

        assertEquals(XMLStreamConstants.START_ELEMENT, xsr.next());
        assertEquals(start, xsr.getName());
        assertEquals(start.getPrefix(), xsr.getPrefix());
        assertEquals(start.getLocalPart(), xsr.getLocalName());
        assertEquals(start.getNamespaceURI(), xsr.getNamespaceURI());
        assertEquals(XMLStreamConstants.CHARACTERS, xsr.next()); // this is the last event we saved
        assertEquals(text, xsr.getText());
        assertEquals(XMLStreamConstants.END_ELEMENT, xsr.next());  
        asserter.assertDocumentEnd(xsr);
        
        xsr.reset();

        assertEquals(XMLStreamConstants.START_ELEMENT, xsr.next());
        assertEquals(start, xsr.getName());
        assertEquals(XMLStreamConstants.CHARACTERS, xsr.next()); // this is the last event we saved
        assertEquals(text, xsr.getText());
        assertEquals(XMLStreamConstants.END_ELEMENT, xsr.next());  
        assertEquals(XMLStreamConstants.END_DOCUMENT, xsr.next());
    }
    
    @Test
    public void testFullReverse() throws Exception
    {
        ReversibleXMLStreamReader xsr = createReader();
        
        asserter.assertDocumentStart(xsr);
        assertEquals(XMLStreamConstants.CHARACTERS, xsr.next());
        assertEquals(XMLStreamConstants.END_ELEMENT, xsr.next());
        asserter.assertDocumentEnd(xsr);
        
        xsr.reset();

        assertEquals(XMLStreamConstants.START_ELEMENT, xsr.next());
        assertEquals(XMLStreamConstants.CHARACTERS, xsr.next());
        assertEquals(XMLStreamConstants.END_ELEMENT, xsr.next());  
        assertEquals(XMLStreamConstants.END_DOCUMENT, xsr.next());
    }
    
    private ReversibleXMLStreamReader createReader() throws Exception
    {
        XMLInputFactory xif = XMLInputFactory.newInstance();
        XMLStreamReader coreReader = xif.createXMLStreamReader(getClass().getResourceAsStream("/simple.xml"));
        
        //com.ctc.wstx.sr.ValidatingStreamReader
        //com.sun.org.apache.xerces.internal.impl.XMLStreamReaderImpl
        ReversibleXMLStreamReader xsr = new ReversibleXMLStreamReader(coreReader);
        xsr.setTracking(true);
        return xsr;
    }
    
    private static class JaxpStreamReaderAsserter
    {
        public void assertDocumentStart(XMLStreamReader xsr) throws XMLStreamException
        {
            assertEquals(XMLStreamConstants.START_ELEMENT, xsr.next());
        }
        
        public void assertDocumentEnd(XMLStreamReader xsr) throws XMLStreamException
        {
            assertEquals(XMLStreamConstants.END_DOCUMENT, xsr.next());
        }
    }
    
    private static class StaxStreamReaderAsserter extends JaxpStreamReaderAsserter
    {
        @Override
        public void assertDocumentStart(XMLStreamReader xsr) throws XMLStreamException
        {
            assertEquals(XMLStreamConstants.START_ELEMENT, xsr.next());
        }

        @Override
        public void assertDocumentEnd(XMLStreamReader xsr) throws XMLStreamException
        {
            assertEquals(XMLStreamConstants.END_DOCUMENT, xsr.next());
        }
    }
}


