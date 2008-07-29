/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.filters.xml;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.xml.filters.IsXmlFilter;
import org.mule.module.xml.util.XMLTestUtils;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.IOUtils;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

public class IsXmlFilterTestCase extends AbstractMuleTestCase
{

    private IsXmlFilter filter;

    protected void doSetUp() throws Exception
    {
        filter = new IsXmlFilter();
    }

    public void testFilterFalse() throws Exception
    {
        assertFalse(filter.accept(new DefaultMuleMessage("This is definitely not XML.")));
    }

    public void testFilterFalse2() throws Exception
    {
        assertFalse(filter.accept(new DefaultMuleMessage(
            "<line>This is almost XML</line><line>This is almost XML</line>")));
    }

    public void testFilterTrue() throws Exception
    {
        assertTrue(filter.accept(new DefaultMuleMessage("<msg attrib=\"att1\">This is some nice XML!</msg>")));
    }

    public void testFilterBytes() throws Exception
    {
        byte[] bytes = "<msg attrib=\"att1\">This is some nice XML!</msg>".getBytes();
        assertTrue(filter.accept(new DefaultMuleMessage(bytes)));
    }

    public void testFilterNull() throws Exception
    {
        assertFalse(filter.accept(new DefaultMuleMessage(null)));
    }

    public void testFilterLargeXml() throws Exception
    {
        InputStream is = IOUtils.getResourceAsStream("cdcatalog.xml", getClass());
        assertNotNull("Test resource not found.", is);
        final String xml = IOUtils.toString(is);
        assertTrue(filter.accept(new DefaultMuleMessage(xml)));
    }

    public void testFilterLargeXmlCompliantHtml() throws Exception
    {
        InputStream is = IOUtils.getResourceAsStream("cdcatalog.html", getClass());
        assertNotNull("Test resource not found.", is);
        final String html = IOUtils.toString(is);
        assertTrue(filter.accept(new DefaultMuleMessage(html)));
    }

    public void testFilterXmlMessageVariants() throws Exception
    {
        List list = XMLTestUtils.getXmlMessageVariants("cdcatalog.xml");
        Iterator it = list.iterator();
        
        Object msg;
        while (it.hasNext())
        {
            msg = it.next();
            assertTrue(filter.accept(new DefaultMuleMessage(msg)));
        }
    }
}
