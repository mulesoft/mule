/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.utils;


public class IdUtils
{
    public static final String DASH = "-";

    private IdUtils()
    {
    }

    public static String padId(String id)
    {
        if(id.indexOf(DASH) == 1)
        {
            id = "0" + id ;
        }

        return id;
    }

    public static String removePadding (String id)
    {
        if (id!= null && id.indexOf(DASH) == 2 && id.charAt(0) == '0')
        {
            return id.substring(1);
        }

        return id;
    }

}
