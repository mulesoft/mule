/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.stockquote;

import org.mule.config.i18n.MessageFactory;

/**
 * <code>LocaleMessage</code> is a convenience interface for retrieving
 * internationalised strings from resource bundles. The actual work is done by
 * the {@link MessageFactory} in core.
 */
public class LocaleMessage extends MessageFactory
{
    private static final LocaleMessage factory = new LocaleMessage();
    
    /**
     * Note that the messages for this example are not in mule's standard
     * META-INF/services/org/mule/i18n folder but in a different resource bundle.
     */
    private static final String BUNDLE_PATH = "messages.stockquote-example-messages";

    public static String getStockQuoteMessage(String symbol, String name, String date, String last, 
        String change, String open, String high, String low, String volume, String previousClose)
    {
        String[] params = { symbol, name, date, last, change, open, high,
            low, volume, previousClose };
        return factory.getString(BUNDLE_PATH, 1, params);
    }

    @Override
    protected ClassLoader getClassLoader()
    {
        return getClass().getClassLoader();
    }
}
