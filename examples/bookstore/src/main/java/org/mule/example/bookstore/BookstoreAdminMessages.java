/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.bookstore;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class BookstoreAdminMessages extends MessageFactory
{
    private static final BookstoreAdminMessages factory = new BookstoreAdminMessages();
    
    private static final String BUNDLE_PATH = "messages.bookstore-admin-messages";

    public static Message missingAuthor()
    {
        return factory.createMessage(BUNDLE_PATH, 1);
    }

    public static Message missingTitle()
    {
        return factory.createMessage(BUNDLE_PATH, 2);
    }

    public static Message missingPrice()
    {
        return factory.createMessage(BUNDLE_PATH, 3);
    }
}
