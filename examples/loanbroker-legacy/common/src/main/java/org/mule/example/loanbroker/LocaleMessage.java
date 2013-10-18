/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.loanbroker;

import org.mule.config.i18n.MessageFactory;
import org.mule.example.loanbroker.messages.LoanQuote;

public class LocaleMessage extends MessageFactory
{
    private static final LocaleMessage factory = new LocaleMessage();
    
    private static final String BUNDLE_PATH = "messages.loanbroker-example-messages";

    public static String receivedRequest(String[] params)
    {
        return factory.getString(BUNDLE_PATH, 1, params);
    }

    public static String receivedQuote(String[] params)
    {
        return factory.getString(BUNDLE_PATH, 2, params);
    }

    public static String receivedRate(LoanQuote quote)
    {
        return factory.getString(BUNDLE_PATH, 3, quote.toString());
    }

    public static String loanQuote(String bankName, double interestRate)
    {
        return factory.getString(BUNDLE_PATH, 4, bankName, String.valueOf(interestRate));
    }

    public static String processingQuote(LoanQuote quote)
    {
        return factory.getString(BUNDLE_PATH, 5, quote.toString());
    }
    
    public static String lowestQuote(LoanQuote lowestQuote)
    {
        return factory.getString(BUNDLE_PATH, 6, lowestQuote.toString());
    }

    public static String receivedProfile(String[] params)
    {
        return factory.getString(BUNDLE_PATH, 7, params);
    }

    public static String responseNumQuotes(int i)
    {
        return factory.getString(BUNDLE_PATH, 10, String.valueOf(i));
    }

    public static String responseAvgRequest(long l)
    {
        return factory.getString(BUNDLE_PATH, 11, String.valueOf(l));
    }

    public static String requestError()
    {
        return factory.getString(BUNDLE_PATH, 12);
    }

    public static String requestResponse(Object payload)
    {
        return factory.getString(BUNDLE_PATH, 13, payload);
    }

    public static String exiting()
    {
        return factory.getString(BUNDLE_PATH, 14);
    }

    public static String menuError()
    {
        return factory.getString(BUNDLE_PATH, 15);
    }

    public static String enterName()
    {
        return factory.getString(BUNDLE_PATH, 16);
    }

    public static String enterLoanAmount()
    {
        return factory.getString(BUNDLE_PATH, 17);
    }

    public static String enterLoanDuration()
    {
        return factory.getString(BUNDLE_PATH, 18);
    }

    public static String loanDurationError(String duration)
    {
        return factory.getString(BUNDLE_PATH, 19, duration);
    }

    public static String loanAmountError(String amount)
    {
        return factory.getString(BUNDLE_PATH, 20, amount);
    }

    public static String menuOptionNumberOfRequests()
    {
        return factory.getString(BUNDLE_PATH, 22);
    }

    public static String menuErrorNumberOfRequests()
    {
        return factory.getString(BUNDLE_PATH, 23);
    }

    public static String request(int i, Object object)
    {
        return factory.getString(BUNDLE_PATH, 24, String.valueOf(i), object);
    }

    public static String esbWelcome()
    {
        return factory.getString(BUNDLE_PATH, 30);
    }

    public static String loadingEndpointEjb()
    {
        return factory.getString(BUNDLE_PATH, 31);
    }

    public static String loadingManagedEjb()
    {
        return factory.getString(BUNDLE_PATH, 33);
    }

    public static String welcome()
    {
        return factory.getString(BUNDLE_PATH, 40);
    }

    public static String menu()
    {
        return factory.getString(BUNDLE_PATH, 41);
    }

    public static String sentAsync()
    {
        return factory.getString(BUNDLE_PATH, 42);
    }

    public static String menuOptionSoap()
    {
        return factory.getString(BUNDLE_PATH, 43);
    }

    public static String menuOptionMode()
    {
        return factory.getString(BUNDLE_PATH, 44);
    }

    public static String loadingAsync()
    {
        return factory.getString(BUNDLE_PATH, 45);
    }

    public static String loadingSync()
    {
        return factory.getString(BUNDLE_PATH, 46);
    }

    @Override
    protected ClassLoader getClassLoader()
    {
        return getClass().getClassLoader();
    }
}
