/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class JdbcMessages extends MessageFactory
{
    private static final JdbcMessages factory = new JdbcMessages();
    
    private static final String BUNDLE_PATH = getBundlePath("jdbc");

    public static Message transactionSetAutoCommitFailed()
    {
        return factory.createMessage(BUNDLE_PATH, 1);
    }

    public static Message jndiResourceNotFound(String name)
    {
        return factory.createMessage(BUNDLE_PATH, 2, name);
    }
    
    public static Message moreThanOneMessageInTransaction(String property1, String property2)
    {
        return factory.createMessage(BUNDLE_PATH, 3, property1, property2);
    }
    
    public static Message forcePropertyNoTransaction(String property, String transction)
    {
        return factory.createMessage(BUNDLE_PATH, 4, property, transction);
    }
    
    public static Message forceProperty(String property1, String property2)
    {
        return factory.createMessage(BUNDLE_PATH, 5, property1, property2);
    }

}


