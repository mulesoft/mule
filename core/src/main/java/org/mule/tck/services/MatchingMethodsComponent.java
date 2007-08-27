/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.services;

import org.mule.util.StringUtils;

import java.rmi.Remote;

/**
 * A test component that has two service methods with matching signature
 */
public class MatchingMethodsComponent implements Remote
{
    public String reverseString(String string)
    {
        return StringUtils.reverse(string);
    }

    public String upperCaseString(String string)
    {
        return string.toUpperCase();
    }
}
