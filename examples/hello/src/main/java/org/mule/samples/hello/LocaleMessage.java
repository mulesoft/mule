/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.hello;

import org.mule.config.i18n.LocaleMessageHandler;

/**
 * <code>LocaleMessage</code> is a convenience interface for retrieving
 * internationalised strings from resource bundles. The actual work is done by
 * the LocaleMessageHandler in core.
 *
 * The <code>LocaleMessage</code> at minimum provides the same methods in the
 * LocaleMessageHandler except that the bundle name is provided. 
 *
 * Optionally, the LocaleMessage can contain convenience methods for accessing
 * specific string resources so the resource codes don't have to be used directly.
 */
public class LocaleMessage
{
    // The bundle name for this package
    public static String bundleName = "hello-example";

    // Identifies for specific string resources
    public static String GREETING_PART_2 = "1";
    public static String GREETING_PART_1 = "2";
    public static String PROMPT = "3";

    public static String getString(String code)
    {
        return LocaleMessageHandler.getString(bundleName, code);
    }

    public static String getString(String code, Object arg1)
    {
        return LocaleMessageHandler.getString(bundleName, code, arg1);
    }

    public static String getString(String code, Object arg1, Object arg2)
    {
        return LocaleMessageHandler.getString(bundleName, code, arg1, arg2);
    }

    public static String getString(String code, Object[] args)
    {
        return LocaleMessageHandler.getString(bundleName, code, args);
    }

    /* Convenience methods start here */

    public static String getGreetingPart1()
    {
        return LocaleMessageHandler.getString(bundleName, GREETING_PART_1);
    }

    public static String getGreetingPart2()
    {
        return LocaleMessageHandler.getString(bundleName, GREETING_PART_2);
    }

    public static String getPrompt()
    {
        return LocaleMessageHandler.getString(bundleName, PROMPT);
    }

}
