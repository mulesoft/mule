/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml;

import org.mule.api.transformer.Transformer;
import org.mule.module.xml.transformer.DomDocumentToXml;
import org.mule.module.xml.transformer.XmlToDomDocument;
import org.mule.module.xml.util.XMLTestUtils;
import org.mule.module.xml.util.XMLUtils;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.IOUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.dom4j.DocumentHelper;
import org.dom4j.io.DOMWriter;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DomXmlTransformersTestCase extends AbstractXmlTransformerTestCase
{

    private String srcData;
    private Document resultData;

    @Override
    protected void doSetUp() throws Exception
    {
        srcData = IOUtils.getResourceAsString("cdcatalog.xml", getClass());
        org.dom4j.Document dom4jDoc = DocumentHelper.parseText(srcData);
        resultData = new DOMWriter().write(dom4jDoc);
    }

    @Override
    public Transformer getTransformer() throws Exception
    {
        XmlToDomDocument trans = createObject(XmlToDomDocument.class);
        trans.setReturnDataType(DataTypeFactory.create(org.w3c.dom.Document.class));
        return trans;
    }

    @Override
    public Transformer getRoundTripTransformer() throws Exception
    {
        DomDocumentToXml trans = createObject(DomDocumentToXml.class);
        trans.setReturnDataType(DataTypeFactory.STRING);
        return trans;
    }

    @Override
    public Object getTestData()
    {
        return srcData;
    }

    @Override
    public Object getResultData()
    {
        return resultData;
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
        writeXml((Node) result, System.out);
        assertNotNull(result);
        assertTrue("expected: " + expectedResult + "\nresult: " + result, compareResults(expectedResult, result));
    }

    public static void writeXml(Node n, OutputStream os) throws TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        // identity
        javax.xml.transform.Transformer t = tf.newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.transform(new DOMSource(n), new StreamResult(os));
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
