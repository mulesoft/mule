/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.apache.commons.lang;

/**
 * This class is unfortunately necessary since commons-lang Entity coding support is
 * unnecessarily package-protected and not extensible or customizable otherwise.
 */
// @ThreadSafe
public class MuleEntities
{
    protected static final Entities MuleEntities = new Entities();

    protected static final String[][] BASIC_ARRAY =
    {
        {"quot", "34"}, // " - double-quote
        {"amp", "38"}, // & - ampersand
        {"lt", "60"}, // < - less-than
        {"gt", "62"}, // > - greater-than
    };

    protected static final String[][] APOS_ARRAY =
    {
        {"apos", "39"}, // XML apostrophe
    };

    static
    {
        MuleEntities.addEntities(APOS_ARRAY);
        MuleEntities.addEntities(BASIC_ARRAY);
        MuleEntities.addEntities(Entities.ISO8859_1_ARRAY);
        MuleEntities.addEntities(Entities.HTML40_ARRAY);
    }

    private MuleEntities()
    {
        // no instances
    }

    public static String escape(String str)
    {
        return MuleEntities.escape(str);
    }

    public static String unescape(String str)
    {
        return MuleEntities.unescape(str);
    }

}
