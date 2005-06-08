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
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.tck.AbstractMuleTestCase;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class PayloadTypeFilterTestCase extends AbstractMuleTestCase
{
    public void testPayloadTypeFilter()
    {
        PayloadTypeFilter filter = new PayloadTypeFilter(Exception.class);
        assertNotNull(filter.getExpectedType());
        assertTrue(filter.accept(new MuleMessage(new Exception("test"), null)));
        assertTrue(!filter.accept(new MuleMessage("test", null)));

        filter.setExpectedType(String.class);
        assertTrue(filter.accept(new MuleMessage("test", null)));
        assertTrue(!filter.accept(new MuleMessage(new Exception("test"), null)));
    }
}
