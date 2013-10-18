/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Calendar;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@SmallTest
public class NumberUtilsTestCase extends AbstractMuleTestCase
{
    static final long l = 1000000000;

    @Test
    public void testStringToLong()
    {
        assertEquals(l, NumberUtils.toLong("1000000000"));
    }

    @Test
    public void testLongToLong()
    {
        assertEquals(l, NumberUtils.toLong(new Long(l)));
    }

    @Test
    public void testIntegerToLong()
    {
        assertEquals(l, NumberUtils.toLong(new Integer(1000000000)));
    }

    @Test
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

    @Test
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
