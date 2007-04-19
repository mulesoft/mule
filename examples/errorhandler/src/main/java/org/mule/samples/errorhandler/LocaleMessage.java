/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.errorhandler;

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
    public static String bundleName = "errorhandler-example";

    // Identifies for specific string resources
    public static String UNRETRIEVED_EXCEPTION = "1";
    public static String UNHANDLED_EXCEPTION = "2";
    public static String BUSINESS_ERROR_MANAGER_ERROR = "3";
    public static String ERROR_DETAIL = "4";
    public static String ERROR_CLASS = "5";
    public static String HANDLER_FAILURE = "6";
    public static String DEFAULT_FATAL_HANDLING = "7";
    public static String FATAL_HANDLING = "8";
    public static String DEFAULT_HANDLING = "9";
    public static String DEFAULT_EXCEPTION = "10";
    public static String DEFAULT_HANDLER_EXCEPTION = "11";
    public static String FATAL_EXCEPTION = "12";
    public static String BUSINESS_HANDLER_MESSAGE = "13";
    public static String DEFAULT_HANDLER_MESSAGE = "14";
    public static String FATAL_HANDLER_MESSAGE = "15";
    public static String FATAL_HANDLER_EXCEPTION = "16";

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

}
