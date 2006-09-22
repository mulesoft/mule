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

import java.util.List;

import junit.framework.TestCase;

import org.mule.util.ArrayUtils;

public class ArrayUtilsTestCase extends TestCase
{

    public void testToArrayOfComponentType()
    {
        // null array
        assertNull(ArrayUtils.toArrayOfComponentType(null, String.class));

        // empty array, same result
        String[] a = new String[]{};
        String[] a2 = (String[])ArrayUtils.toArrayOfComponentType(a, String.class);
        assertSame(a2, a);

        // null component type is not allowed
        try {
            ArrayUtils.toArrayOfComponentType(a, null);
            fail();
        }
        catch (IllegalArgumentException iex) {
            // ok
        }

        // single element
        a = new String[]{":-)"};
        CharSequence[] cs = (CharSequence[])ArrayUtils.toArrayOfComponentType(a, CharSequence.class);
        assertEquals(a.length, cs.length);
        assertSame(a[0], cs[0]);

        // incompatible element types are not a good idea either
        try {
            ArrayUtils.toArrayOfComponentType(a, List.class);
            fail();
        }
        catch (ArrayStoreException asx) {
            // ok
        }

    }

}
