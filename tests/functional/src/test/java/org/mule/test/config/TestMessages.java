/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.config;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class TestMessages extends MessageFactory
{
    private static final TestMessages factory = new TestMessages();
    
    private static final String BUNDLE_PATH = getBundlePath("test");
    
    public static Message testMessage(String arg1, String arg2, String arg3)
    {
        return factory.createMessage(BUNDLE_PATH, 1, arg1, arg2, arg3);
    }
}


