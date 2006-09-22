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

import java.util.Arrays;

import junit.framework.TestCase;

import org.mule.util.StringUtils;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class StringUtilsTestCase extends TestCase
{

    public void testSplitWithTrimming()
    {
        String[] inputValues = new String[]{"foo", "bar", "baz", "kaboom"};

        String inputString = new StringBuffer(40).append(inputValues[0]).append(" ,").append(
                        ",  ").append(inputValues[1]).append(" ,").append(inputValues[2])
                        .append("  ,  ").append(inputValues[3]).append(" ").toString();

        assertTrue(Arrays.equals(inputValues, StringUtils.split(inputString, ",")));
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
