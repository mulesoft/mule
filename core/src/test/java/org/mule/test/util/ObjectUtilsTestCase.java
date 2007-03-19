/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.util;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.ObjectUtils;
import org.mule.util.StringUtils;

public class ObjectUtilsTestCase extends AbstractMuleTestCase
{

    public void testIdentityToShortString()
    {
        assertEquals("null", ObjectUtils.identityToShortString(null));

        String source = "foo";
        String description = ObjectUtils.identityToShortString(source);
        String[] components = StringUtils.split(description, '@');

        assertNotNull(components);
        assertEquals(2, components.length);
        assertEquals("String", components[0]);
        assertEquals(Integer.toHexString(System.identityHashCode(source)), components[1]);
    }

}
