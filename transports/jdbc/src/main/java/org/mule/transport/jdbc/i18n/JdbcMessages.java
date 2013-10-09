/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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


