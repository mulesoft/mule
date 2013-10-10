/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class EmailMessages extends MessageFactory
{
    private static final EmailMessages factory = new EmailMessages();
    
    private static final String BUNDLE_PATH = getBundlePath("email");

    public static Message routingError()
    {
        return factory.createMessage(BUNDLE_PATH, 3);
    }
}


