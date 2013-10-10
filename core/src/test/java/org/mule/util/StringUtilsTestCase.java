/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SmallTest
public class StringUtilsTestCase extends AbstractMuleTestCase
{

    @Test
    public void testSplitAndTrim1()
    {
        String[] result = StringUtils.splitAndTrim(null, ",,");
        assertNull(result);

        result = StringUtils.splitAndTrim("", ",");
        assertNotNull(result);
        assertTrue(Arrays.equals(ArrayUtils.EMPTY_STRING_ARRAY, result));

        result = StringUtils.splitAndTrim(" ", ",");
        assertNotNull(result);
        assertTrue(Arrays.equals(ArrayUtils.EMPTY_STRING_ARRAY, result));
    }

    @Test
    public void testSplitAndTrim2()
    {
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

    @Test
    public void testSplitAndTrim3()
    {
        String[] inputValues = new String[]{"foo", "bar", "baz", "kaboom"};
        String inputString = "foo,  bar,\nbaz,  \nkaboom";
        assertTrue(Arrays.equals(inputValues, StringUtils.splitAndTrim(inputString, ",")));
    }

    @Test
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

    @Test
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
