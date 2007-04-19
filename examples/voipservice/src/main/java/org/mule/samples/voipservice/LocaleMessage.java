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
    public static String bundleName = "voip-example";

    // Identifies for specific string resources
    public static String WELCOME = "1";
    public static String MENU_OPTION_1 = "2";
    public static String MENU_OPTION_QUIT = "3";
    public static String MENU_PROMPT = "4";
    public static String GOODBYE = "5";
    public static String MENU_ERROR = "6";
    public static String HOUSE_CAPTION = "7";
    public static String STREET_CAPTION = "8";
    public static String CITY_CAPTION = "9";
    public static String CARD_NUMBER_CAPTION = "10";
    public static String VALID_TILL_CAPTION = "11";
    public static String CARD_TYPE_CAPTION = "12";
    public static String FIRST_NAME_CAPTION = "13";
    public static String ADDRESS_CAPTION = "14";
    public static String CUSTOMER_CAPTION = "15";
    public static String CARD_CAPTION = "16";

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

    public static String getWelcomeMessage()
    {
        return getString(WELCOME);
    }

    public static String getMenuOption1()
    {
        return getString(MENU_OPTION_1);
    }

    public static String getMenuOptionQuit()
    {
        return getString(MENU_OPTION_QUIT);
    }

    public static String getMenuPromptMessage()
    {
        return getString(MENU_PROMPT);
    }

    public static String getGoodbyeMessage()
    {
        return getString(GOODBYE);
    }

    public static String getMenuErrorMessage()
    {
        return getString(MENU_ERROR);
    }

    public static String getHouseCaption(Object arg1)
    {
        return getString(HOUSE_CAPTION, arg1);
    }

    public static String getStreetCaption(Object arg1)
    {
        return getString(STREET_CAPTION, arg1);
    }

    public static String getCityCaption(Object arg1)
    {
        return getString(CITY_CAPTION, arg1);
    }

    public static String getCardNumberCaption(Object arg1)
    {
        return getString(CARD_NUMBER_CAPTION, arg1);
    }

    public static String getValidTillCaption(Object arg1)
    {
        return getString(VALID_TILL_CAPTION, arg1);
    }

    public static String getCardTypeCaption(Object arg1)
    {
        return getString(CARD_TYPE_CAPTION, arg1);
    }

    public static String getFirstNameCaption(Object arg1)
    {
        return getString(FIRST_NAME_CAPTION, arg1);
    }

    public static String getAddressCaption(Object arg1)
    {
        return getString(ADDRESS_CAPTION, arg1);
    }

    public static String getCustomerCaption(Object arg1)
    {
        return getString(CUSTOMER_CAPTION, arg1);
    }

    public static String getCardCaption(Object arg1)
    {
        return getString(CARD_CAPTION, arg1);
    }

}
