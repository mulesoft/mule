/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import java.util.StringTokenizer;

/**
 * <code>StringUtils</code> contains useful methods for manipulating Strings.
 */
// @Immutable
public class StringUtils extends org.apache.commons.lang.StringUtils
{

    public static String[] split(String string, String delim)
    {
        StringTokenizer st = new StringTokenizer(string, delim);
        String[] results = new String[st.countTokens()];
        int i = 0;
        while (st.hasMoreTokens())
        {
            results[i++] = st.nextToken().trim();
        }
        return results;
    }

    /**
     * Convert a hexadecimal string into its byte representation.
     * 
     * @param hex The hexadecimal string.
     * @return The converted bytes or <code>null</code> if the hex String is null.
     */
    public static byte[] hexStringToByteArray(String hex)
    {
        if (hex == null)
        {
            return null;
        }

        int stringLength = hex.length();
        if (stringLength % 2 != 0)
        {
            throw new IllegalArgumentException("Hex String must have even number of characters!");
        }

        byte[] result = new byte[stringLength / 2];

        int j = 0;
        for (int i = 0; i < result.length; i++)
        {
            char hi = Character.toLowerCase(hex.charAt(j++));
            char lo = Character.toLowerCase(hex.charAt(j++));
            result[i] = (byte)((Character.digit(hi, 16) << 4) | Character.digit(lo, 16));
        }

        return result;
    }

    /**
     * @see #toHexString(byte[])
     */
    public static String toHexString(byte[] bytes)
    {
        return StringUtils.toHexString(bytes, false);
    }

    /**
     * Convert a byte array to a hexadecimal string.
     * 
     * @param bytes The bytes to format.
     * @param uppercase When <code>true</code> creates uppercase hex characters
     *            instead of lowercase (the default).
     * @return A hexadecimal representation of the specified bytes.
     */
    public static String toHexString(byte[] bytes, boolean uppercase)
    {
        if (bytes == null)
        {
            return null;
        }

        int numBytes = bytes.length;
        StringBuffer str = new StringBuffer(numBytes * 2);

        String table = (uppercase ? HEX_CHARACTERS_UC : HEX_CHARACTERS);

        for (int i = 0; i < numBytes; i++)
        {
            str.append(table.charAt(bytes[i] >>> 4 & 0x0f));
            str.append(table.charAt(bytes[i] & 0x0f));
        }

        return str.toString();
    }

    // lookup tables needed for toHexString(byte[], boolean)
    private static final String HEX_CHARACTERS = "0123456789abcdef";
    private static final String HEX_CHARACTERS_UC = HEX_CHARACTERS.toUpperCase();

}
