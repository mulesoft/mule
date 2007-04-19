/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.loanbroker;

import org.mule.config.i18n.LocaleMessageHandler;

public class LocaleMessage
{
    public static String bundleName = "loanbroker-example";

    public static String RECEIVED_REQUEST = "1";
    public static String RECEIVED_QUOTE = "2";
    public static String RECEIVED_RATE = "3";
    public static String LOAN_QUOTE = "4";
    public static String PROCESSING_QUOTE = "5";
    public static String LOWEST_QUOTE = "6";
    public static String RECEIVED_PROFILE = "7";

    public static String RESPONSE_NUM_QUOTES = "10";
    public static String RESPONSE_AVG_REQUEST = "10";
    public static String REQUEST_ERROR = "12";
    public static String REQUEST_RESPONSE = "13";
    public static String EXITING = "14";
    public static String MENU_ERROR = "15";
    public static String ENTER_NAME = "16";
    public static String ENTER_LOAN_AMT = "17";
    public static String ENTER_LOAN_DURATION = "18";
    public static String LOAN_DURATION_ERROR = "19";
    public static String LOAN_AMT_ERROR = "20";

    public static String MENU_OPTION_NUM_REQUESTS = "22";
    public static String MENU_ERROR_NUM_REQUESTS = "23";
    public static String REQUEST = "24";

    public static String ESB_WELCOME = "30";
    public static String LOADING_ENDPOINT_EJB = "31";
    public static String BYE = "32";
    public static String LOADING_MANAGED_EJB = "33";

    public static String WELCOME = "40";
    public static String MENU = "41";
    public static String SENT_ASYNC = "42";
    public static String MENU_OPTION_SOAP = "43";
    public static String MENU_OPTION_MODE = "44";
    public static String LOADING_ASYNC = "45";
    public static String LOADING_SYNC = "46";

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
