/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.voipservice;

import org.mule.config.i18n.MessageFactory;

/**
 * <code>LocaleMessage</code> is a convenience interface for retrieving
 * internationalised strings from resource bundles. The actual work is done by
 * the MessageFactory in core.
 */
public class LocaleMessage extends MessageFactory
{
    private static final String BUNDLE_PATH = "messages.voip-example-messages";
    
    public static String getWelcomeMessage()
    {
        return getString(BUNDLE_PATH, 1);
    }

    public static String getMenuOption1()
    {
        return getString(BUNDLE_PATH, 2);
    }

    public static String getMenuOptionQuit()
    {
        return getString(BUNDLE_PATH, 3);
    }

    public static String getMenuPromptMessage()
    {
        return getString(BUNDLE_PATH, 4);
    }

    public static String getGoodbyeMessage()
    {
        return getString(BUNDLE_PATH, 5);
    }

    public static String getMenuErrorMessage()
    {
        return getString(BUNDLE_PATH, 6);
    }

    public static String getHouseCaption(Object arg1)
    {
        return getString(BUNDLE_PATH, 7, arg1);
    }

    public static String getStreetCaption(Object arg1)
    {
        return getString(BUNDLE_PATH, 8, arg1);
    }

    public static String getCityCaption(Object arg1)
    {
        return getString(BUNDLE_PATH, 9, arg1);
    }

    public static String getCardNumberCaption(Object arg1)
    {
        return getString(BUNDLE_PATH, 10, arg1);
    }

    public static String getValidTillCaption(Object arg1)
    {
        return getString(BUNDLE_PATH, 11, arg1);
    }

    public static String getCardTypeCaption(Object arg1)
    {
        return getString(BUNDLE_PATH, 12, arg1);
    }

    public static String getFirstNameCaption(Object arg1)
    {
        return getString(BUNDLE_PATH, 13, arg1);
    }

    public static String getAddressCaption(Object arg1)
    {
        return getString(BUNDLE_PATH, 14, arg1);
    }

    public static String getCustomerCaption(Object arg1)
    {
        return getString(BUNDLE_PATH, 15, arg1);
    }

    public static String getCardCaption(Object arg1)
    {
        return getString(BUNDLE_PATH, 16, arg1);
    }
}
