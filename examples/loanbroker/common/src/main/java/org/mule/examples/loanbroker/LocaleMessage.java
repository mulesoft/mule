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

import org.mule.config.i18n.MessageFactory;
import org.mule.examples.loanbroker.messages.LoanQuote;
import org.mule.util.StringMessageUtils;

public class LocaleMessage extends MessageFactory
{
    private static final String BUNDLE_PATH = "messages.loanbroker-example-messages";

    public static String receivedRequest(String[] params)
    {
        return getString(BUNDLE_PATH, 1, StringMessageUtils.toString(params));
    }

    public static String receivedQuote(String[] params)
    {
        return getString(BUNDLE_PATH, 2, StringMessageUtils.toString(params));
    }

    public static String receivedRate(LoanQuote quote)
    {
        return getString(BUNDLE_PATH, 3, quote.toString());
    }

    public static String loanQuote(String bankName, double interestRate)
    {
        return getString(BUNDLE_PATH, 4, bankName, String.valueOf(interestRate));
    }

    public static String processingQuote(LoanQuote quote)
    {
        return getString(BUNDLE_PATH, 5, quote.toString());
    }
    
    public static String lowestQuote(LoanQuote lowestQuote)
    {
        return getString(BUNDLE_PATH, 6, lowestQuote.toString());
    }

    public static String receivedProfile(String[] params)
    {
        return getString(BUNDLE_PATH, 7, StringMessageUtils.toString(params));
    }

    public static String responseNumQuotes(int i)
    {
        return getString(BUNDLE_PATH, 10, String.valueOf(i));
    }

    public static String responseAvgRequest(long l)
    {
        return getString(BUNDLE_PATH, 11, String.valueOf(l));
    }

    public static String requestError()
    {
        return getString(BUNDLE_PATH, 12);
    }

    public static String requestResponse(Object payload)
    {
        return getString(BUNDLE_PATH, 13, payload);
    }

    public static String exiting()
    {
        return getString(BUNDLE_PATH, 14);
    }

    public static String menuError()
    {
        return getString(BUNDLE_PATH, 15);
    }

    public static String enterName()
    {
        return getString(BUNDLE_PATH, 16);
    }

    public static String enterLoanAmount()
    {
        return getString(BUNDLE_PATH, 17);
    }

    public static String enterLoanDuration()
    {
        return getString(BUNDLE_PATH, 18);
    }

    public static String loanDurationError(String duration)
    {
        return getString(BUNDLE_PATH, 19, duration);
    }

    public static String loanAmountError(String amount)
    {
        return getString(BUNDLE_PATH, 20, amount);
    }

    public static String menuOptionNumberOfRequests()
    {
        return getString(BUNDLE_PATH, 22);
    }

    public static String menuErrorNumberOfRequests()
    {
        return getString(BUNDLE_PATH, 23);
    }

    public static String request(int i, Object object)
    {
        return getString(BUNDLE_PATH, 24, String.valueOf(i), object);
    }

    public static String esbWelcome()
    {
        return getString(BUNDLE_PATH, 30);
    }

    public static String loadingEndpointEjb()
    {
        return getString(BUNDLE_PATH, 31);
    }

    public static String loadingManagedEjb()
    {
        return getString(BUNDLE_PATH, 33);
    }

    public static String welcome()
    {
        return getString(BUNDLE_PATH, 40);
    }

    public static String menu()
    {
        return getString(BUNDLE_PATH, 41);
    }

    public static String sentAsync()
    {
        return getString(BUNDLE_PATH, 42);
    }

    public static String menuOptionSoap()
    {
        return getString(BUNDLE_PATH, 43);
    }

    public static String menuOptionMode()
    {
        return getString(BUNDLE_PATH, 44);
    }

    public static String loadingAsync()
    {
        return getString(BUNDLE_PATH, 45);
    }

    public static String loadingSync()
    {
        return getString(BUNDLE_PATH, 46);
    }
}
