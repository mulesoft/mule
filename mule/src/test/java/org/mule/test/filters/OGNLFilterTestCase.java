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
package org.mule.test.filters;

import org.mule.impl.MuleMessage;
import org.mule.routing.filters.OGNLFilter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.test.filters.xml.Dummy;
import org.mule.umo.UMOMessage;

/**
 * @author Holger Hoffstaette
 */

public class OGNLFilterTestCase extends AbstractMuleTestCase
{
    private OGNLFilter filter;

    protected void doSetUp() throws Exception
    {
        filter = new OGNLFilter();
    }

    protected void doTearDown() throws Exception
    {
        filter = null;
    }

    public void testNewFilter()
    {
        assertFalse(filter.accept(null));
    }

    public void testNoExpressionEmptyMessage()
    {
        UMOMessage message = new MuleMessage(null);

        assertFalse(filter.accept(message));
    }

    public void testNoExpressionValidMessage()
    {
        UMOMessage message = new MuleMessage("foo");

        assertFalse(filter.accept(message));
    }

    public void testStringExpression()
    {
        UMOMessage message = new MuleMessage("foo");
        filter.setExpression("equals(\"foo\")");                                     

        assertTrue(filter.accept(message));
    }

    public void testObjectExpression()
    {
        Dummy payload = new Dummy();
        payload.setContent("foobar");
        UMOMessage message = new MuleMessage(payload);
        filter.setExpression("content.endsWith(\"bar\")");

        assertTrue(filter.accept(message));
    }

}
