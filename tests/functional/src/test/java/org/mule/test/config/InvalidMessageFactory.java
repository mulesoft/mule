/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.config;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class InvalidMessageFactory extends MessageFactory
{
    private static final InvalidMessageFactory factory = new InvalidMessageFactory();
    
    private static final String BUNDLE_PATH = getBundlePath("thisdoesnotexist");
    
    public static Message getInvalidMessage()
    {
        // the code can safely be ignored. MessageFactory must fail before when
        // trying to find the inexistent bundle.
        return factory.createMessage(BUNDLE_PATH, 42);
    }
}


