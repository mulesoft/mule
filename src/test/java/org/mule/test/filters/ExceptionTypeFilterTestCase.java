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
import org.mule.impl.message.ExceptionPayload;
import org.mule.routing.filters.ExceptionTypeFilter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOMessage;

import java.io.IOException;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ExceptionTypeFilterTestCase extends AbstractMuleTestCase
{
    public void testPayloadTypeFilter()
    {
        ExceptionTypeFilter filter = new ExceptionTypeFilter();
        assertNull(filter.getExpectedType());
        UMOMessage m = new MuleMessage("test", null);
        assertTrue(!filter.accept(m));
        m.setExceptionPayload(new ExceptionPayload(new NullPointerException("test")));
        assertTrue(filter.accept(m));

        filter = new ExceptionTypeFilter(IOException.class);
        assertTrue(!filter.accept(m));
        m.setExceptionPayload(new ExceptionPayload(new IOException("test")));
        assertTrue(filter.accept(m));
    }
}
