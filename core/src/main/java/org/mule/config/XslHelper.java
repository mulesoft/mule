/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config;

import org.mule.util.StringMessageUtils;
import org.mule.util.UUID;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple object for colecting and reporting error messages
 */
public class XslHelper
{
    private static List errors = new ArrayList();

    public static void reportError(String message)
    {
        errors.add((errors.size() +1) + ". " + message);
    }

    public static boolean hasErrorReport()
    {
        return errors.size() > 0;
    }

    public static String getErrorReport()
    {
        return StringMessageUtils.getBoilerPlate(errors, '#', 76);
    }

    public static void clearErrors()
    {
        errors.clear();
    }

    public static String getUniqueId()
    {
        return UUID.getUUID();
    }

    public static String concatId(String string)
    {
        return string + "#" + UUID.getUUID();
    }
}
