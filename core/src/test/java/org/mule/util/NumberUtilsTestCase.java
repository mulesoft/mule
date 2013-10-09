/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
