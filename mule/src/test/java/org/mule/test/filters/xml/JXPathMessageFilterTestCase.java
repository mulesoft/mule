/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.filters.xml;

import org.apache.commons.io.IOUtils;
import org.mule.impl.MuleMessage;
import org.mule.routing.filters.xml.JXPathMessageFilter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transformers.xml.XmlToDomDocument;
import org.mule.umo.UMOMessage;

import java.io.InputStream;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class JXPathMessageFilterTestCase extends AbstractMuleTestCase
{
    private String xmlData = null;

    private JXPathMessageFilter myFilter = null;
    private XmlToDomDocument transformer = null;

    protected void doSetUp() throws Exception
    {
        final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        final InputStream is = currentClassLoader.getResourceAsStream("cdcatalog.xml");
        assertNotNull("Test resource not found.", is);

        xmlData = IOUtils.toString(is);

        // new UMOFilter
        myFilter = new JXPathMessageFilter();
        transformer = new XmlToDomDocument();
    }

    public void testFilter1() throws Exception
    {
        Object obj = transformer.transform(xmlData);
        UMOMessage message = new MuleMessage(obj);
        myFilter.setExpression("payload/catalog/cd[3]/title");
        myFilter.setValue("Greatest Hits");
        boolean res = myFilter.accept(message);
        assertTrue(res);

    }

    public void testFilter2() throws Exception
    {
        Object obj = transformer.transform(xmlData);
        UMOMessage message = new MuleMessage(obj);
        myFilter.setExpression("(payload/catalog/cd[3]/title) ='Greatest Hits'");
        boolean res = myFilter.accept(message);
        assertTrue(res);

    }

    public void testFilter3() throws Exception
    {
        Object obj = transformer.transform(xmlData);
        UMOMessage message = new MuleMessage(obj);
        myFilter.setExpression("count(payload/catalog/cd) = 26");
        boolean res = myFilter.accept(message);
        assertTrue(res);

    }

    public void testFilter4() throws Exception
    {
        Dummy d = new Dummy();
        d.setId(10);
        d.setContent("hello");
        UMOMessage message = new MuleMessage(d);
        myFilter.setExpression("payload/id=10 and payload/content='hello'");
        boolean res = myFilter.accept(message);
        assertTrue(res);

    }
}
