/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
