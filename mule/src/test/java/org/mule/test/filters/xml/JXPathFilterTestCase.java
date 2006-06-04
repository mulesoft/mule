/*
 * $Id$
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

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.dom4j.InvalidXPathException;
import org.mule.impl.MuleMessage;
import org.mule.routing.filters.xml.JXPathFilter;
import org.mule.tck.AbstractMuleTestCase;

/**
 * @author <a href="mailto:S.Vanmeerhaege@gfdi.be">Vanmeerhaeghe St?phane</a>
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 * @version $Revision$
 */
public class JXPathFilterTestCase extends AbstractMuleTestCase {
    private String xmlData = null;

    private JXPathFilter myFilter = null;

    protected void doSetUp() throws Exception {
        // Read Xml file
        final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        final InputStream is = currentClassLoader.getResourceAsStream("cdcatalog.xml");
        assertNotNull("Test resource not found.", is);
        xmlData = IOUtils.toString(is);

        myFilter = new JXPathFilter();
    }

    public void testFilter1() throws Exception {
        myFilter.setExpression("catalog/cd[3]/title");
        myFilter.setExpectedValue("Greatest Hits");
        assertTrue(myFilter.accept(new MuleMessage(xmlData)));
    }

    public void testFilter2() throws Exception {
        myFilter.setExpression("(catalog/cd[3]/title) ='Greatest Hits'");
        assertTrue(myFilter.accept(new MuleMessage(xmlData)));
    }

    public void testFilter3() throws Exception {
        myFilter.setExpression("count(catalog/cd) = 26");
        assertTrue(myFilter.accept(new MuleMessage(xmlData)));
    }

    public void testFilter4() throws Exception {
        Dummy d = new Dummy();
        d.setId(10);
        d.setContent("hello");

        myFilter.setExpression("id=10 and content='hello'");
        assertTrue(myFilter.accept(new MuleMessage(d)));
    }

    public void testBogusExpression() throws Exception {
        try {
            myFilter.setExpression("catalog/cd[3]/banana");
            myFilter.accept(new MuleMessage(xmlData));
            // fail("Invalid XPath should have thrown an exception");
        } catch (InvalidXPathException e) {
            // expected
        }
    }
}
