/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.rmi.i18n;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

public class RmiMessages extends MessageFactory
{
    private static final RmiMessages factory = new RmiMessages();
    
    private static final String BUNDLE_PATH = getBundlePath("rmi");

    public static Message messageParamServiceMethodNotSet()
    {
        return factory.createMessage(BUNDLE_PATH, 1);
    }

    public static Message messageReceiverNeedsRmiAble()
    {
        return factory.createMessage(BUNDLE_PATH, 10);
    }

    public static Message serviceClassInvocationFailed()
    {
        return factory.createMessage(BUNDLE_PATH, 11);
    }
}


