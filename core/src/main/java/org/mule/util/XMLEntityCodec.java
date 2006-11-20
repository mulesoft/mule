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

import org.apache.commons.lang.MuleEntities;

// @ThreadSafe
public class XMLEntityCodec
{

    public static String encodeString(String str)
    {
        if (StringUtils.isEmpty(str))
        {
            return str;
        }

        return MuleEntities.escape(str);
    }

    public static String decodeString(String str)
    {
        if (StringUtils.isEmpty(str))
        {
            return str;
        }

        return MuleEntities.unescape(str);
    }

}
