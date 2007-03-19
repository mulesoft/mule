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

import java.util.Calendar;

import junit.framework.TestCase;

import org.mule.util.NumberUtils;

public class NumberUtilsTestCase extends TestCase
{
    static final long l = 1000000000;

    public void testStringToLong()
    {
        assertEquals(l, NumberUtils.toLong("1000000000"));
    }

    public void testLongToLong()
    {
        assertEquals(l, NumberUtils.toLong(new Long(l)));
    }

    public void testIntegerToLong()
    {
        assertEquals(l, NumberUtils.toLong(new Integer(1000000000)));
    }

    public void testIncompatible()
    {
        try
        {
            NumberUtils.toLong(Calendar.getInstance().getTime());
            fail();
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }
    }

    public void testNull()
    {
        try
        {
            // need to cast to Object, otherwise compiler would resolve method to
            // superclass' implementation
            NumberUtils.toLong((Object)null);
            fail();
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }
    }

}
