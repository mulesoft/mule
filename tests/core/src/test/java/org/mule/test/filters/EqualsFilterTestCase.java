/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.filters;

import org.mule.routing.filters.EqualsFilter;
import org.mule.tck.AbstractMuleTestCase;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class EqualsFilterTestCase extends AbstractMuleTestCase
{
    public void testEqualsFilter()
    {
        Exception obj = new Exception("test");
        EqualsFilter filter = new EqualsFilter(obj);
        assertNotNull(filter.getPattern());
        assertTrue(filter.accept(obj));
        assertTrue(!filter.accept(new Exception("tes")));

        filter.setPattern("Hello");
        assertTrue(filter.accept("Hello"));
        assertTrue(!filter.accept("Helo"));
    }
}
