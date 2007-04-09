/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.filters.xml;

import org.mule.impl.MuleMessage;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class IsXmlFilterTestCase extends AbstractMuleTestCase
{

    private IsXmlFilter filter;

    protected void doSetUp() throws Exception
    {
        filter = new IsXmlFilter();
    }

    public void testFilterFalse() throws Exception
    {
        assertFalse(filter.accept(new MuleMessage("This is definitely not XML.")));
    }

    public void testFilterFalse2() throws Exception
    {
        assertFalse(filter.accept(new MuleMessage(
            "<line>This is almost XML</line><line>This is almost XML</line>")));
    }

    public void testFilterTrue() throws Exception
    {
        assertTrue(filter.accept(new MuleMessage("<msg attrib=\"att1\">This is some nice XML!</msg>")));
    }

    public void testFilterBytes() throws Exception
    {
        byte[] bytes = "<msg attrib=\"att1\">This is some nice XML!</msg>".getBytes();
        assertTrue(filter.accept(new MuleMessage(bytes)));
    }

    public void testFilterNull() throws Exception
    {
        assertFalse(filter.accept(new MuleMessage(null)));
    }

    public void testFilterLargeXml() throws Exception
    {
        final String xml = loadFromClasspath("cdcatalog.xml");
        assertTrue(filter.accept(new MuleMessage(xml)));
    }

    public void testFilterLargeXmlFalse() throws Exception
    {
        final String html = loadFromClasspath("cdcatalog.html");
        assertTrue(filter.accept(new MuleMessage(html)));
    }

    private String loadFromClasspath(final String name) throws IOException
    {
        InputStream is = IOUtils.getResourceAsStream(name, getClass());
        assertNotNull("Test resource not found.", is);

        return IOUtils.toString(is);
    }

}
