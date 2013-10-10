/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.hello;

import org.mule.config.i18n.MessageFactory;

/**
 * <code>LocaleMessage</code> is a convenience interface for retrieving
 * internationalised strings from resource bundles. The actual work is done by
 * the MessageFactory in core.
 */
public class LocaleMessage extends MessageFactory
{
    private static final LocaleMessage factory = new LocaleMessage();
    
    private static final String BUNDLE_PATH = "messages.hello-example-messages";

    public static String getGreetingPart2()
    {
        return factory.getString(BUNDLE_PATH, 1);
    }

    public static String getGreetingPart1()
    {
        return factory.getString(BUNDLE_PATH, 2);
    }

    public static String getPrompt()
    {
        return factory.getString(BUNDLE_PATH, 3);
    }

    public static String getInvalidUserNameError()
    {
        return factory.getString(BUNDLE_PATH, 4);
    }
}
