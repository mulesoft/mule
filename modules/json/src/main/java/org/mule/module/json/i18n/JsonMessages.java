/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
