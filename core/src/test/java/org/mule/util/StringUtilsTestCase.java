/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import org.mule.tck.AbstractMuleTestCase;

import java.util.Arrays;

public class StringUtilsTestCase extends AbstractMuleTestCase
{

    public void testSplitWithTrimming()
    {
        String[] result = StringUtils.splitAndTrim(null, ",,");
        assertNull(result);

        result = StringUtils.splitAndTrim("", ",");
        assertNotNull(result);
        assertTrue(Arrays.equals(ArrayUtils.EMPTY_STRING_ARRAY, result));

        result = StringUtils.splitAndTrim(" ", ",");
        assertNotNull(result);
        assertTrue(Arrays.equals(new String[]{""}, result));

        String[] inputValues = new String[]{"foo", "bar", "baz", "kaboom"};
        String inputString = new StringBuffer(40)
            .append(inputValues[0])
            .append(" ,")
            .append(",  ")
            .append(inputValues[1])
            .append(" ,")
            .append(inputValues[2])
            .append("  ,  ")
            .append(inputValues[3])
            .append(" ")
            .toString();

        assertTrue(Arrays.equals(inputValues, StringUtils.splitAndTrim(inputString, ",")));
    }

    public void testHexStringToByteArray()
    {
        assertNull(StringUtils.hexStringToByteArray(null));

        try
        {
            StringUtils.hexStringToByteArray("1");
            fail();
        }
        catch (IllegalArgumentException iex)
        {
            // OK
        }

        assertTrue(Arrays.equals(new byte[]{}, StringUtils.hexStringToByteArray("")));
        assertTrue(Arrays.equals(new byte[]{1}, StringUtils.hexStringToByteArray("01")));
        assertTrue(Arrays.equals(new byte[]{1, 2}, StringUtils.hexStringToByteArray("0102")));
        assertTrue(Arrays.equals(new byte[]{10, 14}, StringUtils.hexStringToByteArray("0A0E")));
        assertTrue(Arrays.equals(new byte[]{10, 14}, StringUtils.hexStringToByteArray("0a0e")));
        assertTrue(Arrays.equals(new byte[]{10, (byte)0xff}, StringUtils.hexStringToByteArray("0AFF")));
        assertTrue(Arrays.equals(new byte[]{10, (byte)0xff}, StringUtils.hexStringToByteArray("0aff")));
    }

    public void testByteArrayToHexString()
    {
        assertNull(StringUtils.toHexString(null));
        assertEquals("", StringUtils.toHexString(new byte[]{}));
        assertEquals("01", StringUtils.toHexString(new byte[]{1}));
        assertEquals("0102", StringUtils.toHexString(new byte[]{1, 2}));
        assertEquals("0a0e", StringUtils.toHexString(new byte[]{10, 14}));
        assertEquals("0A0E", StringUtils.toHexString(new byte[]{10, 14}, true));
        assertEquals("0aff", StringUtils.toHexString(new byte[]{10, (byte)0xff}));
        assertEquals("0AFF", StringUtils.toHexString(new byte[]{10, (byte)0xff}, true));
    }

}
