/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.filters.xml;

import org.mule.DefaultMuleMessage;
import org.mule.module.xml.filters.JXPathFilter;
import org.mule.module.xml.util.XMLTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.InputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.io.IOUtils;
import org.dom4j.DocumentHelper;
import org.junit.Test;
import org.xml.sax.InputSource;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JXPathFilterTestCase extends AbstractMuleContextTestCase
{
    private String xmlStringInput = null;
    private String xmlStringInputNS = null;
    private org.dom4j.Document dom4jDocumentInput = null;
    private org.dom4j.Document dom4jDocumentInputNS = null;
    private org.w3c.dom.Document w3cDocumentInput = null;
    private org.w3c.dom.Document w3cDocumentInputNS = null;
    private JXPathFilter simpleFilter = null;
    private JXPathFilter nsAwareFilter = null;

    // @SuppressWarnings("unchecked")
    protected void doSetUp() throws Exception
    {
        final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();

        // Read No-Namespace Xml file
        InputStream is = currentClassLoader.getResourceAsStream("cdcatalog.xml");
        assertNotNull("Test resource 'cdcatalog.xml' not found.", is);
        xmlStringInput = IOUtils.toString(is);
        dom4jDocumentInput = DocumentHelper.parseText(xmlStringInput);
        w3cDocumentInput = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new InputSource(new StringReader(xmlStringInput)));
        simpleFilter = new JXPathFilter();
        simpleFilter.setMuleContext(muleContext);

        // Read Namespace Xml file
        is = currentClassLoader.getResourceAsStream("cdcatalogNS.xml");
        assertNotNull("Test resource 'cdcatalogNS.xml' not found.", is);
        xmlStringInputNS = IOUtils.toString(is);
        dom4jDocumentInputNS = DocumentHelper.parseText(xmlStringInputNS);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        w3cDocumentInputNS = dbf.newDocumentBuilder().parse(
                new InputSource(new StringReader(xmlStringInputNS)));
        nsAwareFilter = new JXPathFilter();
        Map namespaces = new HashMap();
        namespaces.put("nsone", "http://one.org");
        namespaces.put("nstwo", "http://two.org");
        nsAwareFilter.setNamespaces(namespaces);
        nsAwareFilter.setMuleContext(muleContext);
        muleContext.start();
    }

    @Test
    public void testBogusExpression() throws Exception
    {
        try
        {
            JXPathFilter myFilter = createObject(JXPathFilter.class);
            myFilter.setPattern("foo/bar/");
            myFilter.accept(new DefaultMuleMessage(xmlStringInput, muleContext));
            fail("Invalid XPath should have thrown an exception");
        }
        //Now we have Jaxen on the class path we get a Jaxen exception, but this is an unchecked exception
        catch (Exception e)
        {
            // expected
        }
    }

    private void doTestExpectedValueFilter(Object xmlData) throws Exception
    {
        simpleFilter.setPattern("catalog/cd[3]/title");
        simpleFilter.setExpectedValue("Greatest Hits");
        assertTrue(simpleFilter.accept(new DefaultMuleMessage(xmlData, muleContext)));
    }

    private void doTestBooleanFilter1(Object xmlData) throws Exception
    {
        simpleFilter.setPattern("(catalog/cd[3]/title) ='Greatest Hits'");
        assertTrue(simpleFilter.accept(new DefaultMuleMessage(xmlData, muleContext)));
    }

    private void doTestBooleanFilter2(Object xmlData) throws Exception
    {
        simpleFilter.setPattern("count(catalog/cd) = 26");
        assertTrue(simpleFilter.accept(new DefaultMuleMessage(xmlData, muleContext)));
    }

    private void doTestExpectedValueFilterNS(Object xmlData) throws Exception
    {
        nsAwareFilter.setPattern("nsone:catalog/nstwo:cd[3]/title");
        nsAwareFilter.setExpectedValue("Greatest Hits");
        assertTrue(nsAwareFilter.accept(new DefaultMuleMessage(xmlData, muleContext)));
    }

    private void doTestBooleanFilter1NS(Object xmlData) throws Exception
    {
        nsAwareFilter.setPattern("(nsone:catalog/nstwo:cd[3]/title) ='Greatest Hits'");
        assertTrue(nsAwareFilter.accept(new DefaultMuleMessage(xmlData, muleContext)));
    }

    private void doTestBooleanFilter2NS(Object xmlData) throws Exception
    {
        nsAwareFilter.setPattern("count(nsone:catalog/nstwo:cd) = 26");
        assertTrue(nsAwareFilter.accept(new DefaultMuleMessage(xmlData, muleContext)));
    }

    @Test
    public void testFilterOnObject() throws Exception
    {
        Dummy d = new Dummy();
        d.setId(10);
        d.setContent("hello");

        simpleFilter.setPattern("id=10 and content='hello'");
        assertTrue(simpleFilter.accept(new DefaultMuleMessage(d, muleContext)));
    }

    @Test
    public void testExpectedValueFilterXmlString() throws Exception
    {
        doTestExpectedValueFilter(xmlStringInput);
    }

    @Test
    public void testExpectedValueFilterXmlByteArray() throws Exception
    {
        doTestExpectedValueFilter(xmlStringInput.getBytes());
    }

    @Test
    public void testBooleanFilter1XmlString() throws Exception
    {
        doTestBooleanFilter1(xmlStringInput);
    }

    @Test
    public void testBooleanFilter2XmlString() throws Exception
    {
        doTestBooleanFilter2(xmlStringInput);
    }

    @Test
    public void testExpectedValueFilterDom4JDocument() throws Exception
    {
        doTestExpectedValueFilter(dom4jDocumentInput);
    }

    @Test
    public void testBooleanFilter1Dom4JDocument() throws Exception
    {
        doTestBooleanFilter1(dom4jDocumentInput);
    }

    @Test
    public void testBooleanFilter2Dom4JDocument() throws Exception
    {
        doTestBooleanFilter2(dom4jDocumentInput);
    }

    @Test
    public void testExpectedValueFilterW3cDocument() throws Exception
    {
        doTestExpectedValueFilter(w3cDocumentInput);
    }

    @Test
    public void testBooleanFilter1W3cDocument() throws Exception
    {
        doTestBooleanFilter1(w3cDocumentInput);
    }

    @Test
    public void testBooleanFilter2W3cDocument() throws Exception
    {
        doTestBooleanFilter2(w3cDocumentInput);
    }

    @Test
    public void testExpectedValueFilterXmlStringNS() throws Exception
    {
        doTestExpectedValueFilterNS(xmlStringInputNS);
    }

    @Test
    public void testBooleanFilter1XmlStringNS() throws Exception
    {
        doTestBooleanFilter1NS(xmlStringInputNS);
    }

    @Test
    public void testBooleanFilter2XmlStringNS() throws Exception
    {
        doTestBooleanFilter2NS(xmlStringInputNS);
    }

    @Test
    public void testExpectedValueFilterDom4JDocumentNS() throws Exception
    {
        doTestExpectedValueFilterNS(dom4jDocumentInputNS);
    }

    @Test
    public void testBooleanFilter1Dom4JDocumentNS() throws Exception
    {
        doTestBooleanFilter1NS(dom4jDocumentInputNS);
    }

    @Test
    public void testBooleanFilter2Dom4JDocumentNS() throws Exception
    {
        doTestBooleanFilter2NS(dom4jDocumentInputNS);
    }

    @Test
    public void testExpectedValueFilterW3cDocumentNS() throws Exception
    {
        doTestExpectedValueFilterNS(w3cDocumentInputNS);
    }

    @Test
    public void testBooleanFilter1W3cDocumentNS() throws Exception
    {
        doTestBooleanFilter1NS(w3cDocumentInputNS);
    }

    @Test
    public void testBooleanFilter2W3cDocumentNS() throws Exception
    {
        doTestBooleanFilter2NS(w3cDocumentInputNS);
    }

    @Test
    public void testSimpleFilterXmlMessageVariants() throws Exception
    {
        simpleFilter.setPattern("catalog/cd[3]/title");
        simpleFilter.setExpectedValue("Greatest Hits");
        
        List list = XMLTestUtils.getXmlMessageVariants("cdcatalog.xml");
        Iterator it = list.iterator();
        
        Object msg;
        while (it.hasNext())
        {
            msg = it.next();
            // TODO Not working for XMLStreamReader
            if (!(msg instanceof XMLStreamReader))
            {
                assertTrue("Test failed for message type: " + msg.getClass(), simpleFilter.accept(new DefaultMuleMessage(msg, muleContext)));
            }
        }
    }
}
