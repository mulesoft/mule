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
package org.mule.test.filters;

import org.mule.impl.MuleMessage;
import org.mule.routing.filters.OGNLFilter;
import org.mule.tck.NamedTestCase;
import org.mule.test.filters.xml.Dummy;
import org.mule.umo.UMOMessage;

/**
 * @author Holger Hoffstaette
 */

public class OGNLFilterTestCase extends NamedTestCase
{
    private OGNLFilter filter;

    protected void setUp() throws Exception
    {
        super.setUp();
        filter = new OGNLFilter();
    }

    protected void tearDown() throws Exception
    {
        filter = null;
        super.tearDown();
    }

    public void testNewFilter()
    {
        assertFalse(filter.accept(null));
    }

    public void testNoExpressionEmptyMessage()
    {
        UMOMessage message = new MuleMessage(null, null);

        assertFalse(filter.accept(message));
    }

    public void testNoExpressionValidMessage()
    {
        UMOMessage message = new MuleMessage("foo", null);

        assertFalse(filter.accept(message));
    }

    public void testStringExpression()
    {
        UMOMessage message = new MuleMessage("foo", null);
        filter.setExpression("equals(\"foo\")");                                     

        assertTrue(filter.accept(message));
    }

    public void testObjectExpression()
    {
        Dummy payload = new Dummy();
        payload.setContent("foobar");
        UMOMessage message = new MuleMessage(payload, null);
        filter.setExpression("content.endsWith(\"bar\")");

        assertTrue(filter.accept(message));
    }

}
