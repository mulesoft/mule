/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.services;

import org.mule.util.StringUtils;

import java.rmi.Remote;

/**
 * A test service that has two service methods with matching signature
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
