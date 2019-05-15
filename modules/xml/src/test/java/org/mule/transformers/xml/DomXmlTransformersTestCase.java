/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.module.xml.transformer.XmlToDomDocument;
import org.mule.module.xml.util.XMLTestUtils;
import org.mule.module.xml.util.XMLUtils;
import org.mule.util.IOUtils;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLStreamReader;

import org.dom4j.DocumentHelper;
import org.dom4j.io.DOMWriter;
import org.junit.Test;
import org.w3c.dom.Node;

public class DomXmlTransformersTestCase extends AbstractDomXmlTransformersTestCase
{

    @Override
    protected void doSetUp() throws Exception
    {
        srcData = IOUtils.getResourceAsString("cdcatalog.xml", getClass());
        org.dom4j.Document dom4jDoc = DocumentHelper.parseText(srcData);
        resultData = new DOMWriter().write(dom4jDoc);
    }

    @Test
    public void testTransformXMLStreamReader() throws Exception
    {
        Object expectedResult = getResultData();
        assertNotNull(expectedResult);
        
        XmlToDomDocument transformer = (XmlToDomDocument) getTransformer();
        
        InputStream is = IOUtils.getResourceAsStream("cdcatalog.xml", XMLTestUtils.class);
        XMLStreamReader sr = XMLUtils.toXMLStreamReader(transformer.getXMLInputFactory(), is);

        Object result = transformer.transform(sr);
        writeXml((Node) result);
        assertNotNull(result);
        assertTrue("expected: " + expectedResult + "\nresult: " + result, compareResults(expectedResult, result));
    }
    
    @Test
    public void testAllXmlMessageTypes() throws Exception
    {
        List list = XMLTestUtils.getXmlMessageVariants("cdcatalog.xml");
        Iterator it = list.iterator();
        
        Object expectedResult = getResultData();
        assertNotNull(expectedResult);
        
        Object msg, result;
        while (it.hasNext())
        {
            msg = it.next();
            result = getTransformer().transform(msg);
            assertNotNull(result);
            assertTrue("Test failed for message type: " + msg.getClass(), compareResults(expectedResult, result));
        }
    }

}
