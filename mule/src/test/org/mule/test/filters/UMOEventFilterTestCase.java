/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.filters;

import org.mule.routing.filters.UMOEventFilter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOEvent;

/**
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class UMOEventFilterTestCase extends AbstractMuleTestCase
{
    public void testUMOEventFilter() throws Exception
    {
        UMOEvent event = getTestEvent("test");
        UMOEventFilter filter = new UMOEventFilter();
        assertTrue(filter.accept(event));
        assertTrue(!filter.accept("test"));
    }
}
