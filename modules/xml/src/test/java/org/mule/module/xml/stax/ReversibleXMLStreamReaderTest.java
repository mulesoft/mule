/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.xml.stax;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import junit.framework.TestCase;

public class ReversibleXMLStreamReaderTest extends TestCase
{
    public void testReverse() throws Exception
    {
        XMLInputFactory xif = XMLInputFactory.newInstance();
        
        XMLStreamReader coreReader = xif.createXMLStreamReader(getClass().getResourceAsStream("/simple.xml"));
        
        ReversibleXMLStreamReader xsr = new ReversibleXMLStreamReader(coreReader);
        
        xsr.setTracking(true);

        assertEquals(XMLStreamConstants.SPACE, xsr.next());
        assertEquals(XMLStreamConstants.START_ELEMENT, xsr.next());
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
        assertEquals(XMLStreamConstants.SPACE, xsr.next());
        assertEquals(XMLStreamConstants.END_DOCUMENT, xsr.next());

        xsr.reset();

        assertEquals(XMLStreamConstants.START_ELEMENT, xsr.next());
        assertEquals(start, xsr.getName());
        assertEquals(XMLStreamConstants.CHARACTERS, xsr.next()); // this is the last event we saved
        assertEquals(text, xsr.getText());
        assertEquals(XMLStreamConstants.END_ELEMENT, xsr.next());  
        assertEquals(XMLStreamConstants.END_DOCUMENT, xsr.next());
    }
    
    public void testFullReverse() throws Exception
    {
        XMLInputFactory xif = XMLInputFactory.newInstance();
        
        XMLStreamReader coreReader = xif.createXMLStreamReader(getClass().getResourceAsStream("/simple.xml"));
        
        ReversibleXMLStreamReader xsr = new ReversibleXMLStreamReader(coreReader);
        
        xsr.setTracking(true);

        assertEquals(XMLStreamConstants.SPACE, xsr.next());
        assertEquals(XMLStreamConstants.START_ELEMENT, xsr.next());
        assertEquals(XMLStreamConstants.CHARACTERS, xsr.next());
        assertEquals(XMLStreamConstants.END_ELEMENT, xsr.next());   
        assertEquals(XMLStreamConstants.SPACE, xsr.next());     
        assertEquals(XMLStreamConstants.END_DOCUMENT, xsr.next());

        xsr.reset();

        assertEquals(XMLStreamConstants.START_ELEMENT, xsr.next());
        assertEquals(XMLStreamConstants.CHARACTERS, xsr.next());
        assertEquals(XMLStreamConstants.END_ELEMENT, xsr.next());  
        assertEquals(XMLStreamConstants.END_DOCUMENT, xsr.next());
    }
}


