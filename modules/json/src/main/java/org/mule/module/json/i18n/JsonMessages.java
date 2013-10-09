/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.json.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

/**
 * Internationalised messages for the Json module
 */
public class JsonMessages extends MessageFactory
{
    private static final JsonMessages factory = new JsonMessages();

    private static final String BUNDLE_PATH = getBundlePath("json");

    public static Message messageStringIsNotJson()
    {
        return factory.createMessage(BUNDLE_PATH, 1);
    }
}
