/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
